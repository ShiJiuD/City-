package com.cityart.service.impl;

import cn.hutool.json.JSONUtil;
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
    @Lazy
    @Autowired
    private OrderStreamConsumer self;

    /** 单线程消费者执行器 */
    private static final ExecutorService CONSUMER_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * 应用启动时：创建消费者组 + 启动消费者线程
     * <p>
     * XGROUP CREATE stream:orders order-consumer-group 0 MKSTREAM
     */
    @PostConstruct
    private void init() {
        // 创建消费者组（ReadOffset.from("0") 指定从头开始投递）
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
            log.info("消费者组创建成功, stream: {}, group: {}", STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            // 消费者组已存在时 Redis 会报 BUSYGROUP，忽略
            log.info("消费者组已存在, 跳过创建: {}", e.getMessage());
        }

        // 提交消费者任务
        CONSUMER_EXECUTOR.submit(new OrderHandler());
        log.info("Redis Stream 订单消费者已提交");
    }

    /**
     * 应用关闭时停止消费者
     */
    @PreDestroy
    public void stop() {
        CONSUMER_EXECUTOR.shutdown();
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
            while (true) {
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
                    log.error("处理 Stream 消息异常, 进入 pending-list 处理", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
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
            while (true) {
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
                    log.error("处理 pending 消息失败, 稍后重试", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
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
     * 订单落库：解析 Stream 消息，写入数据库
     * <p>
     * 一个事务中完成三件事：
     * 1. INSERT orders
     * 2. 批量 INSERT order_item
     * 3. UPDATE exhibition.sold_count（原子 +delta）
     * <p>
     * 任何一步失败 → 事务回滚 → 消息留在 pending-list → handlePendingList 重试
     *
     * @param value Stream 消息的 field-value 键值对
     */
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(Map<Object, Object> value) {
        // === 1. 从消息中提取基础字段 ===
        String orderNo = String.valueOf(value.get("orderNo"));
        Long userId = Long.valueOf(String.valueOf(value.get("userId")));
        BigDecimal totalAmount = new BigDecimal(String.valueOf(value.get("totalAmount")));

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

        // === 4. Redisson 分布式锁（防止同一用户并发落库导致重复下单） ===
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("获取分布式锁失败，跳过重复订单, userId: {}, orderNo: {}", userId, orderNo);
            return;
        }

        try {
            log.info("开始落库, orderNo: {}, items: {}", orderNo, itemMaps.size());

            // === 5. INSERT orders（下单即已支付，无真实支付网关） ===
            Orders order = new Orders();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setTotalAmount(totalAmount);
            order.setStatus(1); // 已支付
            order.setPayTime(LocalDateTime.now()); // payTime = 订单真正创建的时间
            ordersMapper.insert(order);

            // === 6. 批量 INSERT order_item ===
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

            // === 7. 原子更新已售票数（sold_count + delta），SQL 层 total_stock >= sold_count + delta 保底 ===
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
        } finally {
            lock.unlock();
        }
    }
}
