package com.cityart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求体
 * <p>
 * 一个订单可包含多条明细（不同展览 / 不同票种），
 * 每条明细指定展览ID、票种、数量、观展日期。
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Data
public class CreateOrderDTO {

    /** 订单明细列表，至少 1 条 */
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItem> items;

    /**
     * 订单明细项
     */
    @Data
    public static class OrderItem {

        /** 展览 ID */
        @NotNull(message = "展览ID不能为空")
        private Long exhibitionId;

        /** 票种名称（如"成人票"、"学生票"） */
        @NotBlank(message = "票种不能为空")
        private String ticketType;

        /** 购买数量，至少 1 */
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;

        /** 观展日期/场次时间（格式: yyyy-MM-dd HH:mm:ss） */
        @NotBlank(message = "观展日期不能为空")
        private String visitDate;
    }
}
