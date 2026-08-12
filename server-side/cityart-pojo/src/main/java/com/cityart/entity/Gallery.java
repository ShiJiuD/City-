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
 * 美术馆信息表
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("gallery")
public class Gallery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 美术馆名称
     */
    @TableField("name")
    private String name;

    /**
     * 封面图片URL
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 详细地址
     */
    @TableField("address")
    private String address;

    /**
     * 所在城市（如：南京）
     */
    @TableField("city")
    private String city;

    /**
     * 所在区/县（如：秦淮区）
     */
    @TableField("district")
    private String district;

    /**
     * 美术馆简介
     */
    @TableField("intro")
    private String intro;

    /**
     * 当前在展数量（冗余字段，方便首页直接读取）
     */
    @TableField("exhibition_count")
    private Integer exhibitionCount;

    /**
     * 状态：0-正常营业 1-闭馆维护
     */
    @TableField("status")
    private Integer status;

    /**
     * 美术馆类型：1-综合美术馆 2-当代美术馆 3-古典美术馆 4-雕塑美术馆 5-摄影美术馆
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
