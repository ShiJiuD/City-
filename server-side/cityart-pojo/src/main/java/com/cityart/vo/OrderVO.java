package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应 VO（列表 + 详情共用）
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    /** 订单业务编号（如 20260804153025007） */
    private String orderNo;

    /** 订单状态: 0=待支付, 1=已支付, 2=已取消, 3=已退款 */
    private Integer status;

    /** 实付总金额 */
    private BigDecimal totalAmount;

    /** 下单时间（前端用于计算待支付倒计时） */
    private LocalDateTime createTime;

    /** 支付成功时间（未支付时为 null） */
    private LocalDateTime payTime;

    /** 订单取消时间（未取消时为 null） */
    private LocalDateTime cancelTime;

    /** 退款到账时间（未退款时为 null） */
    private LocalDateTime refundTime;

    /** 订单明细列表（含展览标题、海报等关联字段） */
    private List<OrderItemVO> items;
}
