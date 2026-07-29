package com.cityart.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 展览信息表
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exhibition")
public class Exhibition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属美术馆ID（关联 gallery.id）
     */
    @TableField("gallery_id")
    private Long galleryId;

    /**
     * 展览标题
     */
    @TableField("title")
    private String title;

    /**
     * 副标题/英文标题
     */
    @TableField("subtitle")
    private String subtitle;

    /**
     * 海报/封面图URL
     */
    @TableField("poster_image")
    private String posterImage;

    /**
     * 开始日期
     */
    @TableField("start_date")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @TableField("end_date")
    private LocalDate endDate;

    /**
     * 展览详情介绍
     */
    @TableField("description")
    private String description;

    /**
     * 是否热门/轮播展示：0-否 1-是
     */
    @TableField("is_hot")
    private Integer isHot;

    /**
     * 排序权重（数字越大越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：0-未开始 1-进行中 2-已结束
     */
    @TableField("status")
    private Integer status;

    /**
     * 展览类型（0/1/2... 数字表示）
     */
    @TableField("type")
    private Integer type;

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
