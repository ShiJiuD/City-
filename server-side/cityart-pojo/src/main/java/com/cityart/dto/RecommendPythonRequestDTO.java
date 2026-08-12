package com.cityart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Java → Python 推荐请求体（07 文档 6.1）
 * <p>
 * 字段名与文档契约一一对应：user_profile / candidate_exhibitions 为 snake_case
 * （@JsonProperty 指定），其余 camelCase。改动前必须先改文档。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Data
@Builder
public class RecommendPythonRequestDTO {

    /** 当前请求推荐的用户 ID（游客不发请求，不传） */
    private Long userId;

    /** 用户偏好画像 */
    @JsonProperty("user_profile")
    private UserProfileDTO userProfile;

    /** 可推荐展览候选池，数组内每条为一个展览全量特征 */
    @JsonProperty("candidate_exhibitions")
    private List<CandidateExhibitionDTO> candidateExhibitions;
}
