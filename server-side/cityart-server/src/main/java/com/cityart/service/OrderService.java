package com.cityart.service;

import com.cityart.dto.CreateOrderDTO;
import com.cityart.vo.CreateOrderVO;
import com.cityart.vo.OrderPageVO;
import com.cityart.vo.OrderVO;

/**
 * 订单模块 服务接口
 *
 * @author shijiu
 * @since 2026-08-04
 */
public interface OrderService {

    /**
     * 我的订单列表（分页 + 状态筛选）
     *
     * @param userId 当前登录用户 ID
     * @param status 订单状态筛选（null=全部, 0=待支付, 1=已支付, 2=已取消, 3=已退款）
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 分页订单列表
     */
    OrderPageVO getOrderList(Long userId, Integer status, Integer page, Integer size);

    /**
     * 订单详情
     *
     * @param orderNo 订单业务编号
     * @param userId  当前登录用户 ID（校验归属，防止越权查看）
     * @return 订单详情（含明细 + 展览标题/海报）
     */
    OrderVO getOrderDetail(String orderNo, Long userId);

    /**
     * 创建订单（秒杀模式）
     * <p>
     * Redis Lua 原子扣库存 → XADD Stream → 立即返回，异步落库。
     *
     * @param userId 当前登录用户 ID
     * @param dto    下单请求
     * @return 订单简要信息（订单号、状态、金额、明细，不含支付/取消/退款时间）
     */
    CreateOrderVO createOrder(Long userId, CreateOrderDTO dto);

    /**
     * 取消订单（仅待支付可取消）
     * <p>
     * 状态: 0 → 2，同时回补 Redis 库存。
     *
     * @param orderNo 订单业务编号
     * @param userId  当前登录用户 ID
     */
    void cancelOrder(String orderNo, Long userId);

    /**
     * 申请退款（仅已支付可退款）
     * <p>
     * 状态: 1 → 3，同时回补 Redis 库存。
     *
     * @param orderNo 订单业务编号
     * @param userId  当前登录用户 ID
     */
    void refundOrder(String orderNo, Long userId);
}
