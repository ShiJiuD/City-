package com.cityart.service;

import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.GalleryDetailVO;

/**
 * 详情模块 服务接口
 * <p>
 * 展馆详情 / 展览详情均需登录后访问（JWT 拦截器统一校验）。
 *
 * @author shijiu
 * @since 2026-08-11
 */
public interface DetailService {

    /**
     * 展馆详情（基本信息 + 该馆下展览列表：正在展出、往期）
     *
     * @param id 展馆ID
     * @return 展馆详情 VO
     */
    GalleryDetailVO getGalleryDetail(Long id);

    /**
     * 展览详情（基本信息 + 所属美术馆名称/地址 + 作品集列表）
     *
     * @param id 展览ID
     * @return 展览详情 VO
     */
    ExhibitionDetailVO getExhibitionDetail(Long id);
}
