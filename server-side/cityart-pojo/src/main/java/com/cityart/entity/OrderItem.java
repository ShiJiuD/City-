package com.cityart.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 订单明细 / 票务表实体
 * <p>
 * 一个订单可含多条明细（不同展览 / 不同票种）。
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细主键 ID（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联 orders.id */
    @TableField("order_id")
    private Long orderId;

    /** 关联 exhibition.id（所属展览） */
    @TableField("exhibition_id")
    private Long exhibitionId;

    /** 票种名称（如"成人票"、"学生票"），作为订单快照固化 */
    @TableField("ticket_type")
    private String ticketType;

    /** 购买数量（无符号，不允许负数） */
    @TableField("quantity")
    private Integer quantity;

    /** 商品单价 */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 观展日期 / 场次时间 */
    @TableField("visit_date")
    private LocalDateTime visitDate;

    /** 创建时间（INSERT 时 MyBatis-Plus 自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最近更新时间（INSERT/UPDATE 时 MyBatis-Plus 自动填充） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
