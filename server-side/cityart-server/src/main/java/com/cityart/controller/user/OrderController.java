package com.cityart.controller.user;

import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.context.UserContext;
import com.cityart.dto.CreateOrderDTO;
import com.cityart.result.Result;
import com.cityart.service.OrderService;
import com.cityart.vo.CreateOrderVO;
import com.cityart.vo.OrderPageVO;
import com.cityart.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * C端订单控制器
 * <p>
 * 所有接口需要 JWT 认证（/api/app/ 路径不在拦截器排除名单中），
 * 用户 ID 从 {@link UserContext#getUserId()} 获取。
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Tag(name = "C端订单模块")
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * 我的订单列表（分页 + 状态筛选）
     * <p>
     * 对应前端"我的订单"页面四个 Tab：全部 / 待支付 / 已支付 / 已取消 / 已退款
     *
     * @param status 订单状态（0/1/2/3），不传 = 全部
     * @param page   页码，默认 1
     * @param size   每页条数，默认 10
     */
    @Operation(summary = "获取我的订单列表（分页）")
    @GetMapping("/orders")
    public Result<OrderPageVO> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        log.info("订单列表请求, userId: {}, status: {}, page: {}, size: {}", userId, status, page, size);
        OrderPageVO vo = orderService.getOrderList(userId, status, page, size);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 订单详情
     * <p>
     * 根据订单编号查单个订单，校验归属后返回完整信息（含明细 + 展览标题/海报）。
     *
     * @param orderNo 订单业务编号（路径参数）
     */
    @Operation(summary = "获取订单详情")
    @GetMapping("/orders/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        log.info("订单详情请求, orderNo: {}, userId: {}", orderNo, userId);
        OrderVO vo = orderService.getOrderDetail(orderNo, userId);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 创建订单（秒杀下单入口）
     * <p>
     * 三阶段处理：Redis Lua 扣库存 → XADD Stream → 立即返回<br>
     * 前端收到"下单成功"后，1~2 秒内订单列表可见（异步落库延迟）。
     *
     * @param dto 订单请求体（展览ID、票种、数量、观展日期）
     */
    @Operation(summary = "创建订单")
    @PostMapping("/orders")
    public Result<CreateOrderVO> create(@RequestBody @Validated CreateOrderDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("创建订单请求, userId: {}, items: {}", userId, dto.getItems().size());
        CreateOrderVO vo = orderService.createOrder(userId, dto);
        return Result.success(vo, AuthMessageConstant.ORDER_CREATE_SUCCESS);
    }

    /**
     * 取消订单
     * <p>
     * 仅 status=0（待支付）可取消，取消后回补 Redis 库存。
     *
     * @param orderNo 订单业务编号（路径参数）
     */
    @Operation(summary = "取消订单")
    @PutMapping("/orders/{orderNo}/cancel")
    public Result<?> cancel(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        log.info("取消订单请求, orderNo: {}, userId: {}", orderNo, userId);
        orderService.cancelOrder(orderNo, userId);
        return Result.success(null, AuthMessageConstant.ORDER_CANCEL_SUCCESS);
    }

    /**
     * 申请退款
     * <p>
     * 仅 status=1（已支付）可退款，退款后回补 Redis 库存。
     *
     * @param orderNo 订单业务编号（路径参数）
     */
    @Operation(summary = "申请退款")
    @PutMapping("/orders/{orderNo}/refund")
    public Result<?> refund(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        log.info("退款申请请求, orderNo: {}, userId: {}", orderNo, userId);
        orderService.refundOrder(orderNo, userId);
        return Result.success(null, AuthMessageConstant.ORDER_REFUND_SUCCESS);
    }
}
