package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏数量统计 VO
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCountVO {

    /** 展览收藏数 */
    private Long exhibitionCount;

    /** 美术馆收藏数 */
    private Long galleryCount;

    /** 收藏总数 */
    private Long totalCount;
}
