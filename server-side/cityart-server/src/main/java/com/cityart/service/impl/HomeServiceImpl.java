package com.cityart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cityart.constant.RedisConstant;
import com.cityart.entity.Banner;
import com.cityart.entity.Gallery;
import com.cityart.mapper.BannerMapper;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.HomeService;
import com.cityart.utils.RedisCacheUtil;
import com.cityart.vo.BannerVO;
import com.cityart.vo.GalleryVO;
import com.cityart.vo.HomeExhibitionVO;
import com.cityart.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 首页聚合数据 服务实现类
 * </p>
 * <p>
 * 缓存策略（Cache Aside，通用能力见 {@link RedisCacheUtil}）：<br>
 * 1. 查 Redis → 命中直接返回；<br>
 * 2. 未命中 → 互斥锁（setnx）重建，双检后查库写回；<br>
 * 3. TTL + 随机抖动（防雪崩）；锁 TTL 兜底防持锁方崩溃（防击穿）；<br>
 * 4. 空结果也缓存：恶意 city 参数无法打穿到数据库（防穿透）。
 *
 * @author shijiu
 * @since 2026-07-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HomeServiceImpl implements HomeService {

    /** 获取互斥锁失败后的重试次数上限（50ms × 20 = 1s） */
    private static final int LOCK_RETRY_TIMES = 20;
    /** 锁等待重试间隔 50ms */
    private static final long LOCK_RETRY_INTERVAL = 50L;

    private final BannerMapper bannerMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final GalleryMapper galleryMapper;
    private final RedisCacheUtil redisCacheUtil;

    @Override
    public HomeVO getHomeData(String city) {
        // key 按 city 维度区分：无 city 参数时统一用 "all"，避免参数差异导致缓存不命中
        String cacheKey = RedisConstant.KEY_HOME_CACHE + (StringUtils.hasText(city) ? city : "all");

        // 1. 查缓存，命中直接返回
        HomeVO cached = redisCacheUtil.get(cacheKey, HomeVO.class);
        if (cached != null) {
            log.info("首页缓存命中, key: {}", cacheKey);
            return cached;
        }

        // 2. 缓存未命中 → 互斥锁重建（防缓存击穿）
        String lockKey = RedisConstant.KEY_LOCK_HOME + (StringUtils.hasText(city) ? city : "all");
        String lockValue = redisCacheUtil.tryLock(lockKey);
        if (lockValue == null) {
            // 拿不到锁：说明有其他线程正在重建，休眠后重试
            ThreadUtil.sleep(LOCK_RETRY_INTERVAL);
            HomeVO again = redisCacheUtil.get(cacheKey, HomeVO.class); // 重试前先查缓存，防其他线程重建
            // 缓存已重建，直接返回新数据
            if (again != null) {
                log.info("首页等待锁期间缓存已重建, key: {}", cacheKey);
                return again;
            }
            return getHomeData(city); // 递归重试（最多 1s 内锁 TTL 过期自动让位）
        }
        try {
            // 3. 双检：拿到锁后可能已被其他线程重建
            HomeVO again = redisCacheUtil.get(cacheKey, HomeVO.class);
            if (again != null) {
                return again;
            }
            // 4. 重建：查库组装（原逻辑）
            HomeVO homeVO = buildHomeData(city);
            // 5. 写缓存（TTL + 随机抖动；空结果也缓存，防恶意 city 穿透）
            redisCacheUtil.set(cacheKey, homeVO, RedisConstant.HOME_CACHE_TTL);
            return homeVO;
        } finally {
            // 6. 释放锁（判等删除，防误删他人锁）
            redisCacheUtil.unlock(lockKey, lockValue);
        }
    }

    /**
     * 查库组装首页数据（原逻辑，无缓存路径）
     *
     * @param city 城市筛选（可选）
     * @return 首页聚合数据
     */
    private HomeVO buildHomeData(String city) {
        log.info("获取首页聚合数据, city: {}", city);

        // 1. 查询 Banner，按 sort 降序
        LambdaQueryWrapper<Banner> bannerWrapper = Wrappers.lambdaQuery();
        bannerWrapper.orderByDesc(Banner::getSort);
        List<Banner> banners = bannerMapper.selectList(bannerWrapper);
        List<BannerVO> bannerVOs = banners.stream()
                .map(b -> BeanUtil.copyProperties(b, BannerVO.class))
                .collect(Collectors.toList());
        log.info("Banner 查询完毕，共 {} 条", bannerVOs.size());

        // 2. 查询热门展览（is_hot=1 且当前在展期内），联表拿美术馆名称
        List<HomeExhibitionVO> hotExhibitions = exhibitionMapper.selectHotExhibitions();
        log.info("热门展览查询完毕，共 {} 条", hotExhibitions.size());

        // 3. 查询美术馆（status=0 正常营业），可选城市筛选
        LambdaQueryWrapper<Gallery> galleryWrapper = Wrappers.lambdaQuery();
        galleryWrapper.eq(Gallery::getStatus, 0);
        if (StringUtils.hasText(city)) {
            galleryWrapper.eq(Gallery::getCity, city);
        }
        galleryWrapper.orderByDesc(Gallery::getExhibitionCount);
        List<Gallery> galleries = galleryMapper.selectList(galleryWrapper);
        List<GalleryVO> galleryVOs = galleries.stream()
                .map(g -> BeanUtil.copyProperties(g, GalleryVO.class))
                .collect(Collectors.toList());
        log.info("美术馆查询完毕，共 {} 条", galleryVOs.size());

        // 4. 组装返回
        return HomeVO.builder()
                .banners(bannerVOs)
                .hotExhibitions(hotExhibitions)
                .galleries(galleryVOs)
                .build();
    }
}
