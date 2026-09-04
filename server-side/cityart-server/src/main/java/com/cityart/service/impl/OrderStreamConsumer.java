package com.cityart.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityart.entity.OrderItem;
import com.cityart.entity.Orders;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.OrderItemMapper;
import com.cityart.mapper.OrdersMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Redis Stream 订单消费者（守护线程）
 * <p>
 * 启动后持续监听 stream:orders，异步完成订单落库。
 * 参考黑马 Redis 实战篇 §7.6 的 Stream 消息队列模式。
 * <p>
 * 核心流程：
 * <pre>
 *   while (true) {
 *       XREADGROUP → 收到消息 → 解析 JSON → INSERT orders + order_item → XACK
 *       异常 → handlePendingList() → XREADGROUP ... 0 → 重试 → 成功 → XACK
 *   }
 * </pre>
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStreamConsumer {

    /** Redis Stream 队列名 */
    private static final String STREAM_KEY = "stream:orders";
    /** 消费者组名 */
    private static final String GROUP_NAME = "order-consumer-group";
    /** 消费者名（集群多实例时需区分，当前单机用固定名） */
    private static final String CONSUMER_NAME = "order-consumer-1";

    private final StringRedisTemplate stringRedisTemplate;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final RedissonClient redissonClient;

    /**
     * 自引用代理：processOrder 由内部类 OrderHandler 直接调用时会绕过 Spring AOP，
     * 导致 @Transactional 注解失效（事务不开启，部分写入无法回滚）。
     * 通过注入自身代理再调用，保证事务生效。
     */
    @Lazy  // **这个 Bean 不会在启动阶段实例化，第一次被使用 / 注入的时候才创建**。
    @Autowired
    private OrderStreamConsumer self;

    /** 单线程消费者执行器 */
    private static final ExecutorService CONSUMER_EXECUTOR = Executors.newSingleThreadExecutor();

    /** 消费者运行标志：stop() 置 false 后线程退出（shutdown 停不掉 while(true) 循环） */
    private volatile boolean running = true;

    /**
     * 应用启动时：创建消费者组 + 启动消费者线程
     * <p>
     * XGROUP CREATE stream:orders order-consumer-group 0 MKSTREAM
     * (Redis 未就绪时先记录，消费者循环中遇错会自动重建，不阻塞启动)
     */
    @PostConstruct
    private void init() {
        try {
            ensureGroup();
        } catch (Exception e) {
            log.error("初始创建消费者组失败, 消费者循环中将自动重试: {}", e.getMessage());
        }

        // 提交消费者任务
        CONSUMER_EXECUTOR.submit(new OrderHandler());
        log.info("Redis Stream 订单消费者已提交");
    }

    /**
     * 确保消费者组存在（幂等，自愈）
     * <p>
     * Redis 重启后 stream 可能丢失（空 stream 会被自动清理），
     * 此时 XREADGROUP 会报 NOGROUP；本方法带 MKSTREAM 重建，
     * 消费者组已存在时 Redis 报 BUSYGROUP，忽略即可。
     * <p>
     * 注意：spring-data-redis 3.2.0 已移除 CreateGroupOptions，
     * 直接走底层 RedisConnection.xGroupCreate(key, group, offset, mkStream)。
     */
    private void ensureGroup() {
        try {
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.xGroupCreate(stringRedisTemplate.getStringSerializer().serialize(STREAM_KEY),
                        GROUP_NAME, ReadOffset.from("0"), true);
                return null;
            });
            log.info("消费者组创建成功, stream: {}, group: {}", STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("消费者组已存在, stream: {}", STREAM_KEY);
            } else {
                throw new RuntimeException("创建消费者组失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 应用关闭时停止消费者
     * <p>
     * 置 running=false 让循环退出，再 shutdownNow 中断阻塞中的 read/sleep；
     * 否则 while(true) 循环在 Redisson 销毁后仍会继续打 RedissonShutdownException。
     */
    @PreDestroy
    public void stop() {
        running = false;
        CONSUMER_EXECUTOR.shutdownNow();
        log.info("Redis Stream 订单消费者已停止");
    }

    // ==================== 消息处理线程 ====================

    /**
     * 订单消费线程：循环阻塞读取 Stream 新消息
     * <p>
     * XREADGROUP GROUP order-consumer-group order-consumer-1
     *     COUNT 1 BLOCK 2000 STREAMS stream:orders >
     */
    private class OrderHandler implements Runnable {
        @Override
        public void run() {
            // === 启动时先恢复上次 crash 遗留的 pending 消息 ===
            // 消费者崩溃后，已读取但未 ACK 的消息会永远卡在 pending-list，
            // 必须在读新消息之前先清理掉，否则这些订单永远无法落库。
            handlePendingList();

            // === 主循环：阻塞读取新消息 ===
            while (running) {
                try {
                    // 1. XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                    );

                    // 2. 无消息 → 继续循环
                    if (list == null || list.isEmpty()) {
                        continue;
                    }

                    // 3. 解析数据并落库
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    log.info("收到 Stream 消息, id: {}, orderNo: {}", record.getId(), value.get("orderNo"));

                    self.processOrder(value);

                    // 4. XACK 确认
                    stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                    log.info("消息已确认, id: {}", record.getId());

                } catch (Exception e) {
                    // 停止信号：shutdownNow 中断（read 不响应中断时，running=false 也会退出循环）
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    log.error("处理 Stream 消息异常, 进入 pending-list 处理", e);
                    // Redis 重启导致 stream/消费者组丢失时，重建后自愈
                    try {
                        ensureGroup();
                    } catch (Exception ex) {
                        log.error("重建消费者组失败: {}", ex.getMessage());
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    handlePendingList();
                }
            }
        }

        /**
         * 兜底：处理 pending-list 中已读取但未 XACK 的异常消息
         * <p>
         * XREADGROUP GROUP g1 c1 COUNT 1 STREAMS s1 0
         */
        private void handlePendingList() {
            while (running) {
                try {
                    // 1. 从 pending-list 读取（起始 ID = 0）
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
                    );

                    // 2. pending-list 为空 → 退出
                    if (list == null || list.isEmpty()) {
                        break;
                    }

                    // 3. 解析数据并重试落库
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    log.info("处理 pending 消息, id: {}", record.getId());

                    self.processOrder(value);

                    // 4. XACK 确认
                    stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                    log.info("pending 消息已确认, id: {}", record.getId());

                } catch (Exception e) {
                    // 停止信号：shutdownNow 中断（running=false 也会退出循环）
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    log.error("处理 pending 消息失败, 尝试重建消费者组后重试", e);
                    try {
                        ensureGroup();
                    } catch (Exception ex) {
                        log.error("重建消费者组失败: {}", ex.getMessage());
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    // ==================== 私有工具方法 ====================

    /**
     * 解析 Stream 消息中的观展日期，兼容三种形式：
     * <ol>
     *   <li>字符串 ISO 格式（如 2026-07-01T00:00:00）</li>
     *   <li>字符串自定义格式（如 2026-07-01 00:00:00）</li>
     *   <li>数字：epoch 毫秒时间戳（hutool JSONUtil 序列化 LocalDateTime 的产物）</li>
     * </ol>
     *
     * @param visitDate 消息中的 visitDate 字段值
     * @return LocalDateTime
     */
    private LocalDateTime parseVisitDate(Object visitDate) {
        if (visitDate instanceof Number) {
            long epochMilli = ((Number) visitDate).longValue();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
        }
        String str = String.valueOf(visitDate);
        try {
            return LocalDateTime.parse(str);    // ISO 格式
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    // ==================== 订单落库 ====================

    /**
     * 订单落库（外层：持锁，不开启事务）
     * <p>
     * 锁的释放必须在事务提交之后，否则并发线程会在事务未提交时进入临界区
     * （finally 先于方法返回，事务提交在方法返回之后）。
     * 因此拆两层：本方法负责加锁/释放锁，事务落库委托给 {@link #processOrderTx}，
     * processOrderTx 返回时事务已提交，finally 再释放锁。
     *
     * @param value Stream 消息的 field-value 键值对
     */
    public void processOrder(Map<Object, Object> value) {
        // === 1. 提取用户 ID（分布式锁粒度） ===
        String orderNo = String.valueOf(value.get("orderNo"));
        Long userId = Long.valueOf(String.valueOf(value.get("userId")));

        // === 2. Redisson 分布式锁（防止同一用户并发落库导致重复下单） ===
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // ⚠️ 不能 return：上层会无条件 XACK，订单将永久丢失（多实例并发消费时触发）。
            // 抛异常 → 消息留在 pending-list 等待重试；重试时若已落库，
            // 幂等判断（processOrderTx 开头查 orderNo）会直接 return，不会重复落库。
            log.error("获取分布式锁失败, 消息留在 pending-list 等待重试, userId: {}, orderNo: {}", userId, orderNo);
            throw new RuntimeException("获取订单分布式锁失败, orderNo: " + orderNo);
        }

        try {
            // 事务方法：返回时事务已提交/回滚
            self.processOrderTx(value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 订单落库（内层：事务方法）
     * <p>
     * 一个事务中完成三件事：
     * 1. INSERT orders
     * 2. 批量 INSERT order_item
     * 3. UPDATE exhibition.sold_count（原子 +delta）
     * <p>
     * 任何一步失败 → 事务回滚 → 消息留在 pending-list → handlePendingList 重试
     * <p>
     * 注意：通过 {@code self} 代理调用，保证 @Transactional 生效。
     *
     * @param value Stream 消息的 field-value 键值对
     */
    @Transactional(rollbackFor = Exception.class)
    public void processOrderTx(Map<Object, Object> value) {
        // === 1. 从消息中提取基础字段 ===
        String orderNo = String.valueOf(value.get("orderNo"));
        Long userId = Long.valueOf(String.valueOf(value.get("userId")));
        BigDecimal totalAmount = new BigDecimal(String.valueOf(value.get("totalAmount")));

        // ========== 幂等判断，放在事务最开头 ==========
        // 重复消费（pending 重试 / Redis 重启后消息重投）时按 orderNo 查重：
        // 已存在说明这条消息之前已处理完成，直接 return 正常结束，上层执行 XACK 出队。
        // ⚠️ 不能抛异常：抛异常会进消费者 catch 不执行 XACK，消息留在 pending 无限重试刷日志。
        Orders existOrder = ordersMapper.selectOne(
                new LambdaQueryWrapper<Orders>().eq(Orders::getOrderNo, orderNo));
        if (existOrder != null) {
            log.info("幂等校验：orderNo 已存在，跳过处理 orderNo={}", orderNo);
            return;
        }

        // === 2. 解析订单明细 JSON ===
        String itemsJson = String.valueOf(value.get("itemsJson"));
        cn.hutool.json.JSONArray array = JSONUtil.parseArray(itemsJson);  // 解析为 JSONArray
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            itemMaps.add(array.getJSONObject(i).getRaw());  // 把 hutool 封装的 JSONObject，转成 JDK 原生 Map<String,Object>
        }

        // === 3. 解析库存映射 JSON ===
        String qtyMapJson = String.valueOf(value.get("qtyMapJson"));
        cn.hutool.json.JSONObject qtyJson = JSONUtil.parseObj(qtyMapJson);
        Map<String, Integer> qtyMap = new HashMap<>();
        for (String key : qtyJson.keySet()) {
            qtyMap.put(key, qtyJson.getInt(key));
        }

        log.info("开始落库, orderNo: {}, items: {}", orderNo, itemMaps.size());

        // === 4. INSERT orders（下单即已支付，无真实支付网关） ===
        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now()); // payTime = 订单真正创建的时间
        ordersMapper.insert(order);

        // === 5. 批量 INSERT order_item ===
        for (Map<String, Object> itemMap : itemMaps) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setExhibitionId(Long.valueOf(String.valueOf(itemMap.get("exhibitionId"))));
            item.setTicketType(String.valueOf(itemMap.get("ticketType")));
            item.setQuantity(Integer.valueOf(String.valueOf(itemMap.get("quantity"))));
            item.setUnitPrice(new BigDecimal(String.valueOf(itemMap.get("unitPrice"))));
            item.setVisitDate(parseVisitDate(itemMap.get("visitDate")));
            orderItemMapper.insert(item);
        }

        // === 6. 原子更新已售票数（sold_count + delta），SQL 层 total_stock >= sold_count + delta 保底 ===
        for (Map.Entry<String, Integer> entry : qtyMap.entrySet()) {
            Long exhibitionId = Long.valueOf(entry.getKey());
            int rows = exhibitionMapper.updateSoldCount(exhibitionId, entry.getValue());
            if (rows == 0) {
                throw new RuntimeException("库存更新失败（total_stock 不足），exhibitionId: "
                        + exhibitionId + ", delta: " + entry.getValue());
            }
            log.info("更新 sold_count, exhibitionId: {}, delta: {}", exhibitionId, entry.getValue());
        }

        log.info("订单落库完成, orderNo: {}, orderId: {}", orderNo, order.getId());
    }
}
