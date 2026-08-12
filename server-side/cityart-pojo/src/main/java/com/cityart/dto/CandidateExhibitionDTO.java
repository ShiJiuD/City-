package com.cityart.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 候选展览特征（Java → Python 请求体 candidate_exhibitions 数组元素，07 文档 6.1）
 * <p>
 * 全部 camelCase，与文档契约一致，Python 按此字段名解析。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Data
@Builder
public class CandidateExhibitionDTO {

    /** 展览主键 ID，AI 最终只返回该 ID 做推荐结果 */
    private Long exhibitionId;

    /** 展览所属美术馆 ID */
    private Long galleryId;

    /** 当前展览自身分类编码（exhibition.type） */
    private Integer exhibitionType;

    /** 所属美术馆分类编码（gallery.type） */
    private Integer galleryType;

    /** 是否热门：0-普通 1-热门 */
    private Integer isHot;

    /** 展览状态（由日期实时计算）：0-未开始 1-进行中 */
    private Integer status;
}
