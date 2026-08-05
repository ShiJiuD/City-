package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页聚合数据 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeVO {

    /**
     * Banner 轮播列表
     */
    private List<BannerVO> banners;

    /**
     * 当前热门展览列表
     */
    private List<HomeExhibitionVO> hotExhibitions;

    /**
     * 热门美术馆列表
     */
    private List<GalleryVO> galleries;
}
