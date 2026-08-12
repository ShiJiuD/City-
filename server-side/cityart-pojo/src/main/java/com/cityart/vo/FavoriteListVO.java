package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收藏列表记录 VO（展览 / 美术馆共用，按 targetType 各取字段）
 * <p>
 * type=1 展览：title / galleryName / coverImage / startDate / endDate / price<br>
 * type=2 美术馆：name / coverImage / address / exhibitionCount
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteListVO {

    /** 收藏记录ID */
    private Long id;

    /** 收藏类型：1-展览 2-美术馆 */
    private Integer targetType;

    /** 收藏目标ID（展览ID或美术馆ID） */
    private Long targetId;

    /** 展览标题（type=1） */
    private String title;

    /** 所属美术馆名称（type=1，联表 exhibition.gallery_id → gallery.name） */
    private String galleryName;

    /** 美术馆名称（type=2） */
    private String name;

    /** 海报/封面图URL */
    private String coverImage;

    /** 详细地址（type=2） */
    private String address;

    /** 当前在展数量（type=2，gallery.exhibition_count 冗余字段） */
    private Integer exhibitionCount;

    /** 开始日期（type=1） */
    private LocalDate startDate;

    /** 结束日期（type=1） */
    private LocalDate endDate;

    /** 票价（type=1，未设置返回 0） */
    private BigDecimal price;

    /** 收藏时间（yyyy-MM-dd HH:mm:ss） */
    private String favoriteTime;
}
