package com.cityart.service;

import com.cityart.vo.HomeVO;

/**
 * <p>
 * 首页聚合数据 服务类
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
public interface HomeService {

    /**
     * 获取首页聚合数据（Banner + 热门展览 + 热门美术馆）
     *
     * @param city 城市筛选，不传则返回全部
     * @return 首页聚合 VO
     */
    HomeVO getHomeData(String city);
}
