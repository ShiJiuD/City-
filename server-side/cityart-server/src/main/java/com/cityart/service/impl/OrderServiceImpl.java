package com.cityart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.constant.RedisConstant;
import com.cityart.dto.CreateOrderDTO;
import com.cityart.entity.Exhibition;
import com.cityart.entity.OrderItem;
import com.cityart.entity.Orders;
import com.cityart.exception.AuthException;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.OrderItemMapper;
import com.cityart.mapper.OrdersMapper;
import com.cityart.service.OrderService;
import com.cityart.utils.RedisIdWorker;
import com.cityart.vo.CreateOrderVO;
import com.cityart.vo.OrderItemVO;
import com.cityart.vo.OrderPageVO;
import com.cityart.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单模块 服务实现类
 * <p>
 * 核心流程（秒杀模式）：<br>
 * 1. Redis Lua 原子校验 + 扣减库存<br>
 * 2. XADD 到 Redis Stream 队列<br>
 * 3. 立即返回"下单成功"给前端<br>
 * 4. 后台消费者线程异步落库（INSERT orders + order_item + UPDATE sold_count）
 * <p>
 * 取消 / 退款时回补 Redis 库存 + 订单状态流转。
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrderService {

    // ==================== 依赖注入 ====================

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final OrderStreamConsumer orderStreamConsumer;
    private final RedisIdWorker redisIdWorker;

    // ==================== Lua 脚本（从 resources/seckill.lua 加载） ====================

    /**
     * 秒杀库存扣减 Lua 脚本
     * <p>
     * KEYS[1] = exhibition:stock:{id}<br>
     * ARGV[1] = 购买数量<br>
     * 返回: 1=扣减成功, 0=库存不足, -1=key不存在（未预热）
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new org.springframework.core.io.ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // ==================== 我的订单列表 ====================

    /**
     * 分页查询我的订单
     * <p>
     * 查询逻辑：
     * <ol>
     *   <li>MyBatis-Plus 分页查 orders（按 create_time 降序）</li>
     *   <li>批量查 order_item（IN order_id）</li>
     *   <li>批量查 exhibition 补齐标题 + 海报</li>
     *   <li>组装 OrderPageVO 返回</li>
     * </ol>
     *
     * @param userId 当前登录用户 ID（从 JWT 中解析）
     * @param status 订单状态筛选，不传 = 全部
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @return 分页订单列表
     */
    @Override
    public OrderPageVO getOrderList(Long userId, Integer status, Integer page, Integer size) {
        log.info("查询订单列表, userId: {}, status: {}, page: {}, size: {}", userId, status, page, size);

        // 1. 构建查询条件：当前用户 + 可选状态筛选 + 时间降序
        LambdaQueryWrapper<Orders> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Orders::getUserId, userId);
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        wrapper.orderByDesc(Orders::getCreateTime);

        // 2. MyBatis-Plus 分页查询
        Page<Orders> pageResult = ordersMapper.selectPage(Page.of(page, size), wrapper);
        List<Orders> ordersList = pageResult.getRecords();

        // 3. 空结果快速返回
        if (ordersList.isEmpty()) {
            return OrderPageVO.builder()
                    .total(pageResult.getTotal())
                    .pages(pageResult.getPages())
                    .current(pageResult.getCurrent())
                    .size(pageResult.getSize())
                    .records(Collections.emptyList())
                    .build();
        }

        // 4. 批量查订单明细（按 order_id 分组）
        List<Long> orderIds = ordersList.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // 5. 批量查涉及的所有展览（补齐标题 + 海报）
        Map<Long, Exhibition> exhibitionMap = getExhibitionMap(allItems);

        // 6. 组装 VO 列表
        List<OrderVO> records = ordersList.stream()
                .map(o -> toOrderVO(o, itemsByOrderId.getOrDefault(o.getId(), Collections.emptyList()), exhibitionMap))
                .collect(Collectors.toList());

        return OrderPageVO.builder()
                .total(pageResult.getTotal())
                .pages(pageResult.getPages())
                .current(pageResult.getCurrent())
                .size(pageResult.getSize())
                .records(records)
                .build();
    }

    // ==================== 订单详情 ====================

    /**
     * 根据订单编号查询订单详情
     * <p>
     * 校验订单归属（防止越权查看他人订单），
     * 然后查明细 + 关联展览信息返回。
     *
     * @param orderNo 订单业务编号
     * @param userId  当前登录用户 ID
     * @return 订单详情（含明细 + 展览标题/海报）
     */
    @Override
    public OrderVO getOrderDetail(String orderNo, Long userId) {
        log.info("查询订单详情, orderNo: {}, userId: {}", orderNo, userId);

        // 1. 查订单并校验归属
        Orders order = getByOrderNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new AuthException(AuthMessageConstant.ORDER_NO_PERMISSION);
        }

        // 2. 查明细 + 关联展览
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        Map<Long, Exhibition> exhibitionMap = getExhibitionMap(items);

        return toOrderVO(order, items, exhibitionMap);
    }

    // ==================== 创建订单（Redis 秒杀核心） ====================

    /**
     * 创建订单 —— 秒杀模式三阶段
     * <p>
     * <b>阶段一：Redis 预检（同步，毫秒级）</b><br>
     * ① 查票价：Redis 缓存优先，miss 时查 exhibition.price 并回写（统一票价，不区分票种）<br>
     * ② Lua 原子校验库存：GET stock >= quantity → DECRBY<br>
     * 　 key 不存在 → DB 懒加载后重试<br>
     * 　 库存不足 → 回滚已扣 key，抛异常
     * <p>
     * <b>阶段二：立即返回（同步）</b><br>
     * ③ 生成订单编号<br>
     * ④ XADD 到 stream:orders<br>
     * ⑤ 立即返回"下单成功"
     * <p>
     * <b>阶段三：异步落库（Redis Stream 消费者）</b><br>
     * ⑥ 消费者 XREADGROUP 读取 → INSERT orders + order_item + UPDATE sold_count → XACK
     *
     * @param userId 当前登录用户 ID
     * @param dto    下单请求（展览ID、票种、数量、观展日期）
     * @return 订单简要信息（订单号、状态、金额、明细）
     */
    @Override
    public CreateOrderVO createOrder(Long userId, CreateOrderDTO dto) {
        log.info("创建订单（Redis预检）, userId: {}, items count: {}", userId, dto.getItems().size());

        BigDecimal totalAmount = BigDecimal.ZERO;                       // 累加总金额
        List<OrderItem> orderItems = new ArrayList<>();                 // 待落库的明细列表
        Map<Long, Integer> itemQuantityMap = new HashMap<>();     // 记录每个展览的扣减量（消费者落库用）
        Map<String, Integer> decrKeys = new HashMap<>();          // 已扣减的 key → quantity（预检失败回滚用）

        // ===== 阶段一：Redis 库存预检 + 原子扣减 =====
        try {
            for (CreateOrderDTO.OrderItem itemDto : dto.getItems()) {
                Long exhibitionId = itemDto.getExhibitionId();
                int quantity = itemDto.getQuantity();  // 购买量

                // ① 查票价：Redis 缓存优先，统一使用 exhibition.price（不区分票种）
                BigDecimal unitPrice = getExhibitionPrice(exhibitionId, itemDto.getTicketType());

                // ② Lua 原子操作：GET + 判 + DECRBY（一次 Redis 往返）
                String stockKey = RedisConstant.KEY_EXHIBITION_STOCK + exhibitionId;
                Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                        List.of(stockKey), String.valueOf(quantity));

                // key 不存在 → 库存未预热，从 DB 加载后重试
                if (result == null || result == -1) {
                    log.info("Redis 库存未预热, exhibitionId: {}, 从 DB 加载", exhibitionId);
                    preloadStock(exhibitionId);
                    result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                            List.of(stockKey), String.valueOf(quantity));
                }

                // 库存不足 → 回滚之前已扣减的 key，抛异常
                if (result == null || result == 0) {
                    rollbackStock(decrKeys);
                    decrKeys.clear();   // 已回滚，防止外层 catch 重复回滚
                    throw new AuthException(AuthMessageConstant.ORDER_STOCK_INSUFFICIENT);
                }

                // 记录已扣减 key + 扣减量（失败时需精确回滚）
                decrKeys.merge(stockKey, quantity, Integer::sum);
                itemQuantityMap.merge(exhibitionId, quantity, Integer::sum);

                // 累加金额
                totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));

                // 构造 OrderItem 明细对象（order_id 等异步落库时回填）
                OrderItem item = new OrderItem();
                item.setExhibitionId(exhibitionId);
                item.setTicketType(itemDto.getTicketType());
                item.setQuantity(quantity);
                item.setUnitPrice(unitPrice);
                item.setVisitDate(parseVisitDate(itemDto.getVisitDate()));
                orderItems.add(item);
            }
        } catch (Exception e) {
            // 预检过程中任何异常（如日期格式错误）都回滚已扣库存，防止库存泄漏
            rollbackStock(decrKeys);
            throw e;
        }

        // ===== 阶段二：生成全局唯一订单号 → XADD 到 Redis Stream → 立即返回 =====
        String orderNo = String.valueOf(redisIdWorker.nextId("order"));
        log.info("Redis 扣减成功, orderNo: {}, XADD 到 Stream", orderNo);

        // 组装 Stream 消息：将订单数据序列化为 key-value 对
        Map<String, String> msgData = new HashMap<>();
        msgData.put("orderNo", orderNo);
        msgData.put("userId", userId.toString());
        msgData.put("totalAmount", totalAmount.toString());
        // Redis Stream 的 XADD 只接受 Map<String, String>，而 OrderItem 是内存里的实体对象，没法直接放进去，必须序列化成 JSON 字符串
        // visitDate 显式格式化为字符串，避免 hutool 把 LocalDateTime 序列化成时间戳数字
        List<Map<String, Object>> itemMaps = orderItems.stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("exhibitionId", i.getExhibitionId());
            m.put("ticketType", i.getTicketType());
            m.put("quantity", i.getQuantity());
            m.put("unitPrice", i.getUnitPrice());
            m.put("visitDate", i.getVisitDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return m;
        }).collect(Collectors.toList());
        msgData.put("itemsJson", cn.hutool.json.JSONUtil.toJsonStr(itemMaps));
        msgData.put("qtyMapJson", cn.hutool.json.JSONUtil.toJsonStr(itemQuantityMap));
        // XADD stream:orders * field value ...
        stringRedisTemplate.opsForStream().add("stream:orders", msgData);

        // 立即返回给前端（DB 落库由消费者异步完成）
        return CreateOrderVO.builder()
                .orderNo(orderNo)
                .status(1) // 下单即已支付
                .totalAmount(totalAmount)
                .createTime(LocalDateTime.now())
                .items(orderItems.stream().map(i -> CreateOrderVO.Item.builder()
                        .exhibitionId(i.getExhibitionId())
                        .ticketType(i.getTicketType())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .visitDate(i.getVisitDate())
                        .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // ==================== 取消订单 ====================

    /**
     * 取消订单（仅待支付状态可取消）
     * <p>
     * 状态流转: 0（待支付）→ 2（已取消）<br>
     * 同时回补 Redis 库存（INCRBY）
     *
     * @param orderNo 订单业务编号
     * @param userId  当前登录用户 ID（校验归属）
     */
    @Override
    public void cancelOrder(String orderNo, Long userId) {
        log.info("取消订单, orderNo: {}, userId: {}", orderNo, userId);

        // 1. 查订单 + 校验归属
        Orders order = getByOrderNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new AuthException(AuthMessageConstant.ORDER_OP_NO_PERMISSION);
        }

        // 2. 仅待支付可取消
        if (order.getStatus() != 0) {
            throw new AuthException(AuthMessageConstant.ORDER_CANNOT_CANCEL);
        }

        // 3. 更新状态为"已取消"，记录取消时间
        order.setStatus(2);
        order.setCancelTime(LocalDateTime.now());
        ordersMapper.updateById(order);

        // 4. 回补 Redis 库存
        restoreStock(order.getId());

        log.info("订单已取消, orderNo: {}", orderNo);
    }

    // ==================== 申请退款 ====================

    /**
     * 申请退款（仅已支付状态可退款）
     * <p>
     * 状态流转: 1（已支付）→ 3（已退款）<br>
     * 同时回补 Redis 库存（INCRBY）
     *
     * @param orderNo 订单业务编号
     * @param userId  当前登录用户 ID（校验归属）
     */
    @Override
    public void refundOrder(String orderNo, Long userId) {
        log.info("申请退款, orderNo: {}, userId: {}", orderNo, userId);

        // 1. 查订单 + 校验归属
        Orders order = getByOrderNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new AuthException(AuthMessageConstant.ORDER_OP_NO_PERMISSION);
        }

        // 2. 仅已支付可退款
        if (order.getStatus() != 1) {
            throw new AuthException(AuthMessageConstant.ORDER_REFUND_ONLY_PAID);
        }

        // 3. 更新状态为"已退款"，记录退款时间
        order.setStatus(3);
        order.setRefundTime(LocalDateTime.now());
        ordersMapper.updateById(order);

        // 4. 回补 Redis 库存
        restoreStock(order.getId());

        log.info("退款已处理, orderNo: {}", orderNo);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 查票价：Redis 缓存优先 → miss 时查 exhibition.price 并回写（统一票价）
     * <p>
     * 无论前端传什么票种，都取该展览的 price（成人票票价），缓存为 String 一展一价。<br>
     * ticketType 参数仅保留用于兼容票种维度（订单明细仍记录票种快照），不参与定价。<br>
     * <b>price 为 NULL 是合法的免费展</b>（全项目接口约定 COALESCE(price,0) 显示为 0），
     * 按 0 元处理可正常下单（相当于免费预约凭证），只有展览不存在才视为配置错误。
     *
     * @param exhibitionId 展览 ID
     * @param ticketType   票种名称（如"成人票"，仅记录展示，不影响价格）
     * @return 票价（免费展返回 0）
     * @throws AuthException 展览不存在
     */
    private BigDecimal getExhibitionPrice(Long exhibitionId, String ticketType) {
        String priceKey = RedisConstant.KEY_EXHIBITION_PRICE + exhibitionId;

        // ① Redis 查（String 缓存：一展一价，不区分票种）
        String cached = stringRedisTemplate.opsForValue().get(priceKey);
        if (cached != null) {
            return new BigDecimal(cached);
        }

        // ② Redis miss → 查 exhibition.price → 回写缓存（带 TTL，改价后最多 10 分钟自愈）
        Exhibition exhibition = exhibitionMapper.selectById(exhibitionId);
        if (exhibition == null) {
            throw new AuthException(AuthMessageConstant.ORDER_EXHIBITION_NO_PRICE);
        }
        // 免费展（price NULL）按 0 元处理，可正常下单
        BigDecimal price = exhibition.getPrice() == null ? BigDecimal.ZERO : exhibition.getPrice();
        stringRedisTemplate.opsForValue().set(priceKey, price.toString(),
                RedisConstant.PRICE_TTL, TimeUnit.MILLISECONDS);
        log.info("票价缓存回填, key: {}, value: {}", priceKey, price);
        return price;
    }

    /**
     * 库存懒预热：Redis key 不存在时，从 DB 加载可售库存
     * <p>
     * Redis SET exhibition:stock:{id} = total_stock - sold_count
     *
     * @param exhibitionId 展览 ID
     */
    private void preloadStock(Long exhibitionId) {
        Exhibition exhibition = exhibitionMapper.selectById(exhibitionId);
        if (exhibition == null || exhibition.getTotalStock() == null) {
            throw new AuthException(AuthMessageConstant.ORDER_EXHIBITION_NO_STOCK);
        }

        // 计算可售库存
        int sold = exhibition.getSoldCount() == null ? 0 : exhibition.getSoldCount();
        int available = exhibition.getTotalStock() - sold;

        String stockKey = RedisConstant.KEY_EXHIBITION_STOCK + exhibitionId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(Math.max(available, 0)));
        log.info("库存预热完成, exhibitionId: {}, available: {}", exhibitionId, available);
    }

    /**
     * 回滚 Redis 库存（预检失败时）
     * <p>
     * 按 key → 扣减量精确回补（INCRBY key quantity），
     * 不是简单的 +1，而是把扣掉的每一张票都还回去。
     *
     * @param keyQtyMap 已扣减的 Redis key → 扣减量
     */
    private void rollbackStock(Map<String, Integer> keyQtyMap) {
        for (Map.Entry<String, Integer> entry : keyQtyMap.entrySet()) {
            stringRedisTemplate.opsForValue().increment(entry.getKey(), entry.getValue());
            log.info("Redis 库存回滚（预检失败）, key: {}, qty: {}", entry.getKey(), entry.getValue());
        }
    }

    /**
     * 解析观展日期：兼容 "yyyy-MM-dd"（视为当天 00:00:00）和 "yyyy-MM-dd HH:mm:ss"
     *
     * @param visitDate 前端传入的日期字符串
     * @return LocalDateTime
     * @throws AuthException 两种格式都不匹配时抛出
     */
    private LocalDateTime parseVisitDate(String visitDate) {
        try {
            return LocalDateTime.parse(visitDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(visitDate).atStartOfDay();
            } catch (DateTimeParseException e2) {
                throw new AuthException(AuthMessageConstant.ORDER_VISIT_DATE_INVALID);
            }
        }
    }

    /**
     * 取消 / 退款时回补 Redis 库存
     * <p>
     * 查该订单的所有 order_item，按 exhibition_id 逐个 INCRBY。<br>
     * 只有 Redis key 已存在时才回补（避免回补到未上架的新展览）。
     *
     * @param orderId 订单主键 ID
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            String stockKey = RedisConstant.KEY_EXHIBITION_STOCK + item.getExhibitionId();
            // 仅当 key 存在时回补（防止将已删除展览的库存写回 Redis）
            String current = stringRedisTemplate.opsForValue().get(stockKey);
            if (current != null) {
                stringRedisTemplate.opsForValue().increment(stockKey, item.getQuantity());
                log.info("Redis 库存回补, key: {}, qty: {}", stockKey, item.getQuantity());
            }
        }
    }

    /**
     * 根据订单编号查订单（统一异常）
     *
     * @param orderNo 订单业务编号
     * @return 订单实体
     * @throws AuthException 订单不存在
     */
    private Orders getByOrderNo(String orderNo) {
        Orders order = ordersMapper.selectOne(
                new LambdaQueryWrapper<Orders>().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw new AuthException(AuthMessageConstant.ORDER_NOT_EXIST);
        }
        return order;
    }

    /**
     * 从明细列表提取涉及的所有展览，批量查库返回 ID→Entity 映射
     *
     * @param items 订单明细列表
     * @return exhibitionId → Exhibition 映射
     */
    private Map<Long, Exhibition> getExhibitionMap(List<OrderItem> items) {
        List<Long> exhibitionIds = items.stream()
                .map(OrderItem::getExhibitionId).distinct().collect(Collectors.toList());
        if (exhibitionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return exhibitionMapper.selectBatchIds(exhibitionIds).stream()
                .collect(Collectors.toMap(Exhibition::getId, e -> e));
    }

    /**
     * Order 实体 + 明细 + 展览信息 → OrderVO
     *
     * @param order          订单实体
     * @param items          订单明细列表
     * @param exhibitionMap  展览 ID → Entity 映射
     * @return 订单 VO（含明细 + 展览标题/海报）
     */
    private OrderVO toOrderVO(Orders order, List<OrderItem> items, Map<Long, Exhibition> exhibitionMap) {
        // 明细列表：补齐展览标题 + 海报
        List<OrderItemVO> itemVOs = items.stream()
                .map(i -> {
                    Exhibition exhibition = exhibitionMap.get(i.getExhibitionId());
                    return OrderItemVO.builder()
                            .exhibitionId(i.getExhibitionId())
                            .exhibitionTitle(exhibition != null ? exhibition.getTitle() : null)
                            .posterImage(exhibition != null ? exhibition.getPosterImage() : null)
                            .visitDate(i.getVisitDate())
                            .ticketType(i.getTicketType())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .build();
                })
                .collect(Collectors.toList());

        return OrderVO.builder()
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createTime(order.getCreateTime())
                .payTime(order.getPayTime())
                .cancelTime(order.getCancelTime())
                .refundTime(order.getRefundTime())
                .items(itemVOs)
                .build();
    }
}
