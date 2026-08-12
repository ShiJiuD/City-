package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 推荐展览 VO（首页"艺览智荐"板块返回）
 * <p>
 * 字段与 HomeExhibitionVO 对齐，额外带 price（未设置返回 0，免费展）。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendExhibitionVO {

    /** 展览 ID */
    private Long id;

    /** 海报/封面图 URL */
    private String posterImage;

    /** 展览标题 */
    private String title;

    /** 副标题/英文标题，可空 */
    private String subtitle;

    /** 所属美术馆名称（联表 gallery.name） */
    private String galleryName;

    /** 展览类型：1-当代 2-古典 3-雕塑 4-摄影 */
    private Integer type;

    /** 票价，未设置时返回 0（免费展） */
    private BigDecimal price;
}
