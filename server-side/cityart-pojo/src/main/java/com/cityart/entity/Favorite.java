package com.cityart.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户收藏表（展览 + 美术馆统一收藏，target_type 区分）
 * </p>
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("favorite")
public class Favorite implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（关联 user.id）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 收藏类型：1-展览 2-美术馆
     */
    @TableField("target_type")
    private Integer targetType;

    /**
     * 收藏目标ID（exhibition.id 或 gallery.id）
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 收藏时间（INSERT 时自动填充；表无 update_time 列，不映射）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
