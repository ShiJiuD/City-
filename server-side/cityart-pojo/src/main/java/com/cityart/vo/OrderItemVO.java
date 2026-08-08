package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细 VO
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemVO {

    /** 展览 ID */
    private Long exhibitionId;

    /** 展览标题（关联 exhibition.title） */
    private String exhibitionTitle;

    /** 展览海报图 URL（关联 exhibition.poster_image） */
    private String posterImage;

    /** 观展日期/场次时间 */
    private LocalDateTime visitDate;

    /** 票种名称：成人票/学生票/儿童票 */
    private String ticketType;

    /** 购买数量 */
    private Integer quantity;

    /** 商品单价 */
    private BigDecimal unitPrice;
}
