package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐候选展览 VO（候选池查询 resultType）
 * <p>
 * 字段对应 07 文档 6.1 candidate_exhibitions 全量特征，
 * galleryName 顺带查出供最终 VO 组装，不传给 Python。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendCandidateVO {

    /** 展览主键 ID */
    private Long exhibitionId;

    /** 展览所属美术馆 ID */
    private Long galleryId;

    /** 展览自身分类编码（exhibition.type） */
    private Integer exhibitionType;

    /** 所属美术馆分类编码（gallery.type） */
    private Integer galleryType;

    /** 是否热门：0-普通 1-热门 */
    private Integer isHot;

    /** 展览状态（由日期实时计算）：0-未开始 1-进行中 */
    private Integer status;

    /** 所属美术馆名称（联表 gallery.name） */
    private String galleryName;
}
