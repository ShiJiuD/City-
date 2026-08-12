package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建订单响应 VO（下单成功立即返回）
 * <p>
 * 字段与接口文档严格一致：订单层不含 payTime/cancelTime/refundTime，
 * 明细不含 exhibitionTitle/posterImage（下单走快速路径，只查票价不查标题/海报）。
 *
 * @author shijiu
 * @since 2026-08-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderVO {

    /** 订单业务编号 */
    private String orderNo;

    /** 订单状态（0=待支付） */
    private Integer status;

    /** 实付总金额 */
    private BigDecimal totalAmount;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 订单明细列表 */
    private List<Item> items;

    /**
     * 创建订单响应明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        /** 展览 ID */
        private Long exhibitionId;

        /** 票种名称：成人票/学生票/儿童票 */
        private String ticketType;

        /** 购买数量 */
        private Integer quantity;

        /** 商品单价 */
        private BigDecimal unitPrice;

        /** 观展日期/场次时间 */
        private LocalDateTime visitDate;
    }
}
