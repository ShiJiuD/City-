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
 * C端用户订单主表实体
 * <p>
 * 表名: orders（注意不是 order，order 是 MySQL 保留字）
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("orders")
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单唯一主键 ID（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单业务编号（如 20260804153025007） */
    @TableField("order_no")
    private String orderNo;

    /** 下单用户 ID（关联 user.id） */
    @TableField("user_id")
    private Long userId;

    /** 实付总金额 */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /** 订单状态: 0=待支付, 1=已支付, 2=已取消, 3=已退款 */
    @TableField("status")
    private Integer status;

    /** 支付成功时间 */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /** 订单取消时间 */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    /** 退款到账时间 */
    @TableField("refund_time")
    private LocalDateTime refundTime;

    /** 下单时间（INSERT 时 MyBatis-Plus 自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最近更新时间（INSERT/UPDATE 时 MyBatis-Plus 自动填充） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
