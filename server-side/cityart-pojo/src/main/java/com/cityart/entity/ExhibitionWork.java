package com.cityart.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 展览作品集表
 * </p>
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exhibition_works")
public class ExhibitionWork implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属展览ID（关联 exhibition.id）
     */
    @TableField("exhibition_id")
    private Long exhibitionId;

    /**
     * 作品名称
     */
    @TableField("title")
    private String title;

    /**
     * 作者
     */
    @TableField("artist")
    private String artist;

    /**
     * 作品图片URL
     */
    @TableField("image")
    private String image;

    /**
     * 作品简介
     */
    @TableField("description")
    private String description;

    /**
     * 排序权重（数字越大越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间（INSERT 时自动填充）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最近更新时间（INSERT 和 UPDATE 时自动填充）
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
