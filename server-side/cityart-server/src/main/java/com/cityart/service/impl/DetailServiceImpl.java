package com.cityart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.RedisConstant;
import com.cityart.entity.Exhibition;
import com.cityart.entity.ExhibitionWork;
import com.cityart.entity.Gallery;
import com.cityart.exception.AuthException;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.ExhibitionWorkMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.DetailService;
import com.cityart.utils.RedisCacheUtil;
import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.ExhibitionWorkVO;
import com.cityart.vo.GalleryDetailVO;
import com.cityart.vo.GalleryExhibitionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 详情模块 服务实现类
 * <p>
 * 缓存策略（Cache Aside，通用能力见 {@link RedisCacheUtil}）：<br>
 * 1. 查 Redis → 命中直接返回；<br>
 * 2. DB 不存在 → 写空值缓存（短 TTL），后续请求直接 404 不再打库（防穿透）；<br>
 * 3. 缓存未命中 → 互斥锁（setnx）重建 + 双检，锁 TTL 兜底防持锁方崩溃（防击穿）；<br>
 * 4. TTL + 随机抖动（防雪崩）。<br>
 * 免费展览 price 返回 0（与列表接口 COALESCE 语义一致）。
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetailServiceImpl implements DetailService {

    /** 获取互斥锁失败后的重试次数上限（50ms × 20 = 1s） */
    private static final int LOCK_RETRY_TIMES = 20;
    /** 锁等待重试间隔 50ms */
    private static final long LOCK_RETRY_INTERVAL = 50L;

    private final GalleryMapper galleryMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final ExhibitionWorkMapper exhibitionWorkMapper;
    private final RedisCacheUtil redisCacheUtil;

    @Override
    public GalleryDetailVO getGalleryDetail(Long id) {
        String cacheKey = RedisConstant.KEY_DETAIL_GALLERY + id;

        // 1. 查缓存（命中返回；空值缓存直接 404 不查库）
        GalleryDetailVO cached = getCacheOr404(cacheKey, GalleryDetailVO.class,
                AuthMessageConstant.GALLERY_NOT_EXIST);
        if (cached != null) {
            log.info("展馆详情缓存命中, id: {}", id);
            return cached;
        }

        // 2. 缓存未命中 → 互斥锁重建（防缓存击穿）
        String lockKey = RedisConstant.KEY_LOCK_DETAIL + "gallery:" + id;
        String lockValue = redisCacheUtil.tryLock(lockKey);
        int retry = 0;
        while (lockValue == null) {
            if (++retry > LOCK_RETRY_TIMES) {
                // 锁等待超时兜底：直接查库，牺牲一次并发查询保证可用性
                log.warn("展馆详情互斥锁等待超时, id: {}, 直接查库", id);
                return buildGalleryDetail(id);
            }
            ThreadUtil.sleep(LOCK_RETRY_INTERVAL);
            // 持锁方可能已重建完成：先查缓存命中直接返回（含空值缓存 404），避免无谓抢锁
            GalleryDetailVO again = getCacheOr404(cacheKey, GalleryDetailVO.class,
                    AuthMessageConstant.GALLERY_NOT_EXIST);
            if (again != null) {
                return again;
            }
            lockValue = redisCacheUtil.tryLock(lockKey);
        }
        try {
            // 3. 双检：拿到锁后可能已被其他线程重建
            GalleryDetailVO again = getCacheOr404(cacheKey, GalleryDetailVO.class,
                    AuthMessageConstant.GALLERY_NOT_EXIST);
            if (again != null) {
                return again;
            }
            // 4. 查库重建；DB 不存在 → 写空值缓存后抛 404
            GalleryDetailVO vo;
            try {
                vo = buildGalleryDetail(id);
            } catch (AuthException e) {
                redisCacheUtil.setEmpty(cacheKey);
                throw e;
            }
            // 5. 写缓存（TTL + 随机抖动）
            redisCacheUtil.set(cacheKey, vo, RedisConstant.DETAIL_CACHE_TTL);
            return vo;
        } finally {
            // 6. 释放锁（判等删除，防误删他人锁）
            redisCacheUtil.unlock(lockKey, lockValue);
        }
    }

    @Override
    public ExhibitionDetailVO getExhibitionDetail(Long id) {
        String cacheKey = RedisConstant.KEY_DETAIL_EXHIBITION + id;

        // 1. 查缓存（命中返回；空值缓存直接 404 不查库）
        ExhibitionDetailVO cached = getCacheOr404(cacheKey, ExhibitionDetailVO.class,
                AuthMessageConstant.EXHIBITION_NOT_EXIST);
        if (cached != null) {
            log.info("展览详情缓存命中, id: {}", id);
            return cached;
        }

        // 2. 缓存未命中 → 互斥锁重建（防缓存击穿）
        String lockKey = RedisConstant.KEY_LOCK_DETAIL + "exhibition:" + id;
        String lockValue = redisCacheUtil.tryLock(lockKey);
        int retry = 0;
        while (lockValue == null) {
            if (++retry > LOCK_RETRY_TIMES) {
                log.warn("展览详情互斥锁等待超时, id: {}, 直接查库", id);
                return buildExhibitionDetail(id);
            }
            ThreadUtil.sleep(LOCK_RETRY_INTERVAL);
            // 持锁方可能已重建完成：先查缓存命中直接返回（含空值缓存 404），避免无谓抢锁
            ExhibitionDetailVO again = getCacheOr404(cacheKey, ExhibitionDetailVO.class,
                    AuthMessageConstant.EXHIBITION_NOT_EXIST);
            if (again != null) {
                return again;
            }
            lockValue = redisCacheUtil.tryLock(lockKey);
        }
        try {
            // 3. 双检：拿到锁后可能已被其他线程重建
            ExhibitionDetailVO again = getCacheOr404(cacheKey, ExhibitionDetailVO.class,
                    AuthMessageConstant.EXHIBITION_NOT_EXIST);
            if (again != null) {
                return again;
            }
            // 4. 查库重建；DB 不存在 → 写空值缓存后抛 404
            ExhibitionDetailVO vo;
            try {
                vo = buildExhibitionDetail(id);
            } catch (AuthException e) {
                redisCacheUtil.setEmpty(cacheKey);
                throw e;
            }
            // 5. 写缓存（TTL + 随机抖动）
            redisCacheUtil.set(cacheKey, vo, RedisConstant.DETAIL_CACHE_TTL);
            return vo;
        } finally {
            redisCacheUtil.unlock(lockKey, lockValue);
        }
    }

    // ==================== 缓存私有工具方法 ====================

    /**
     * 从缓存读取并反序列化为 VO
     *
     * @param cacheKey     缓存 key
     * @param type         VO 类型
     * @param notExistMsg  空值缓存命中时抛出的 404 消息
     * @return VO；未命中 / 缓存损坏返回 null
     * @throws AuthException 空值缓存命中（DB 中不存在，不再查库）
     */
    private <T> T getCacheOr404(String cacheKey, Class<T> type, String notExistMsg) {
        String json = redisCacheUtil.getRaw(cacheKey);
        if (json == null) {
            return null;
        }
        if (json.isEmpty()) {
            throw new AuthException(notExistMsg);
        }
        return redisCacheUtil.parse(json, type);
    }

    // ==================== 查库组装（原逻辑） ====================

    /**
     * 查库组装展馆详情（不含缓存逻辑）
     *
     * @param id 展馆 ID
     * @return 展馆详情 VO
     * @throws AuthException 展馆不存在
     */
    private GalleryDetailVO buildGalleryDetail(Long id) {
        log.info("查询展馆详情, id: {}", id);

        Gallery gallery = galleryMapper.selectById(id);
        if (gallery == null) {
            throw new AuthException(AuthMessageConstant.GALLERY_NOT_EXIST);
        }

        // 该馆下所有展览，按排序权重降序；未开始的展览不展示
        LambdaQueryWrapper<Exhibition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Exhibition::getGalleryId, id)
                .orderByDesc(Exhibition::getSortOrder);

        List<Exhibition> exhibitions = exhibitionMapper.selectList(wrapper);

        LocalDate today = LocalDate.now();
        List<GalleryExhibitionVO> current = new ArrayList<>();
        List<GalleryExhibitionVO> past = new ArrayList<>();
        for (Exhibition e : exhibitions) {
            if (e.getStartDate().isAfter(today)) {
                continue; // 未开始，不展示
            }
            GalleryExhibitionVO vo = BeanUtil.copyProperties(e, GalleryExhibitionVO.class);
            vo.setPrice(e.getPrice() != null ? e.getPrice() : BigDecimal.ZERO);
            if (e.getEndDate().isBefore(today)) {
                past.add(vo);
            } else {
                current.add(vo);
            }
        }
        log.info("展馆详情查询完毕, id: {}, 当前展出 {} 条, 往期 {} 条", id, current.size(), past.size());

        return GalleryDetailVO.builder()
                .id(gallery.getId())
                .name(gallery.getName())
                .coverImage(gallery.getCoverImage())
                .address(gallery.getAddress())
                .intro(gallery.getIntro())
                .exhibitionCount(gallery.getExhibitionCount())
                .currentExhibitions(current)
                .pastExhibitions(past)
                .build();
    }

    /**
     * 查库组装展览详情（不含缓存逻辑）
     *
     * @param id 展览 ID
     * @return 展览详情 VO
     * @throws AuthException 展览不存在
     */
    private ExhibitionDetailVO buildExhibitionDetail(Long id) {
        log.info("查询展览详情, id: {}", id);

        Exhibition exhibition = exhibitionMapper.selectById(id);
        if (exhibition == null) {
            throw new AuthException(AuthMessageConstant.EXHIBITION_NOT_EXIST);
        }

        // 联表取美术馆名称/地址
        Gallery gallery = galleryMapper.selectById(exhibition.getGalleryId());

        // 作品集按排序权重降序
        LambdaQueryWrapper<ExhibitionWork> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExhibitionWork::getExhibitionId, id)
                .orderByDesc(ExhibitionWork::getSortOrder);
        List<ExhibitionWork> works = exhibitionWorkMapper.selectList(wrapper);
        List<ExhibitionWorkVO> workVOs = works.stream()
                .map(w -> BeanUtil.copyProperties(w, ExhibitionWorkVO.class))
                .collect(Collectors.toList());
        log.info("展览详情查询完毕, id: {}, 作品 {} 条", id, workVOs.size());

        return ExhibitionDetailVO.builder()
                .id(exhibition.getId())
                .title(exhibition.getTitle())
                .subtitle(exhibition.getSubtitle())
                .posterImage(exhibition.getPosterImage())
                .galleryName(gallery != null ? gallery.getName() : null)
                .galleryAddress(gallery != null ? gallery.getAddress() : null)
                .startDate(exhibition.getStartDate())
                .endDate(exhibition.getEndDate())
                .price(exhibition.getPrice() != null ? exhibition.getPrice() : BigDecimal.ZERO)
                .description(exhibition.getDescription())
                .works(workVOs)
                .build();
    }
}
