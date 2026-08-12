package com.cityart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户偏好画像（Java → Python 请求体 user_profile，07 文档 6.1）
 * <p>
 * 内部协议字段为 snake_case，与前端 camelCase 规范不同属预期，勿改。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Data
@Builder
public class UserProfileDTO {

    /** 用户浏览/收藏过的展览分类 type（exhibition.type） */
    @JsonProperty("history_exhibition_types")
    private List<Integer> historyExhibitionTypes;

    /** 用户浏览/收藏展览所属美术馆分类 type（gallery.type） */
    @JsonProperty("history_gallery_types")
    private List<Integer> historyGalleryTypes;

    /** 用户收藏过的展览 ID，强偏好行为 */
    @JsonProperty("fav_exhibition_ids")
    private List<Long> favExhibitionIds;

    /** 用户浏览过的展览 ID，弱偏好行为；本期恒为空数组（暂无浏览数据源） */
    @JsonProperty("browse_exhibition_ids")
    private List<Long> browseExhibitionIds;
}
