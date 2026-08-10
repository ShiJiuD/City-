package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏状态检查 VO
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCheckVO {

    /** 是否已收藏 */
    private Boolean isFavorited;
}
