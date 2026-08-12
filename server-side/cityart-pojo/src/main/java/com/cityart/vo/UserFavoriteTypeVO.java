package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户收藏展览的类型画像（画像组装查询 resultType）
 * <p>
 * 一次联表查出收藏展览 ID + 展览类型 + 所属美术馆类型，
 * Service 内部分别去重后组装 user_profile 三列表。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFavoriteTypeVO {

    /** 收藏的展览 ID（favorite.target_id） */
    private Long favExhibitionId;

    /** 展览类型（exhibition.type，1-当代 2-古典 3-雕塑 4-摄影） */
    private Integer exhibitionType;

    /** 所属美术馆类型（gallery.type，1-综合 2-当代 3-古典 4-雕塑 5-摄影），LEFT JOIN 可能为空 */
    private Integer galleryType;
}
