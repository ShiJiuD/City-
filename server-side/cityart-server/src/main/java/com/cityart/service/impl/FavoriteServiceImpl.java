package com.cityart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cityart.constant.AuthMessageConstant;
import com.cityart.dto.FavoriteDTO;
import com.cityart.entity.Favorite;
import com.cityart.exception.AuthException;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.FavoriteMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.FavoriteService;
import com.cityart.vo.FavoriteCheckVO;
import com.cityart.vo.FavoriteCountVO;
import com.cityart.vo.FavoriteListVO;
import com.cityart.vo.FavoritePageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 收藏模块 服务实现类
 * <p>
 * 幂等策略：添加/取消都先查唯一键 (user_id, target_type, target_id)，
 * 已收藏/未收藏时返回 false，由 Controller 返回"已收藏"/"未收藏"提示（不报错）。
 * 并发重复收藏由数据库唯一索引 uk_user_target 兜底，捕获 DuplicateKeyException。
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    // ==================== 常量 ====================

    /** 收藏类型：1-展览 */
    private static final int TARGET_TYPE_EXHIBITION = 1;
    /** 收藏类型：2-美术馆 */
    private static final int TARGET_TYPE_GALLERY = 2;
    /** 列表每页条数上限 */
    private static final int MAX_PAGE_SIZE = 50;

    // ==================== 依赖注入 ====================

    private final ExhibitionMapper exhibitionMapper;
    private final GalleryMapper galleryMapper;

    // ==================== 添加收藏 ====================

    /**
     * 添加收藏（幂等）
     * <p>
     * 流程：类型校验 → 唯一键查重（已收藏返回 false）→ 目标存在校验 → INSERT。
     *
     * @param userId 当前登录用户 ID
     * @param dto    收藏请求
     * @return true=新增收藏；false=已收藏过
     */
    @Override
    public boolean addFavorite(Long userId, FavoriteDTO dto) {
        log.info("添加收藏, userId: {}, targetType: {}, targetId: {}",
                userId, dto.getTargetType(), dto.getTargetId());

        checkTargetType(dto.getTargetType());

        // 幂等：已收藏直接返回 false（Controller 返回"已收藏"提示）
        if (getFavorite(userId, dto.getTargetType(), dto.getTargetId()) != null) {
            return false;
        }

        // 校验收藏目标存在（展览/美术馆）
        checkTargetExist(dto.getTargetType(), dto.getTargetId());

        // INSERT（唯一索引 uk_user_target 兜底并发重复）
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(dto.getTargetType());
        favorite.setTargetId(dto.getTargetId());
        try {
            save(favorite);
        } catch (DuplicateKeyException e) {
            log.info("并发重复收藏, userId: {}, targetType: {}, targetId: {}",
                    userId, dto.getTargetType(), dto.getTargetId());
            return false;
        }
        log.info("收藏成功, favoriteId: {}", favorite.getId());
        return true;
    }

    // ==================== 取消收藏 ====================

    /**
     * 取消收藏（幂等）
     * <p>
     * 流程：类型校验 → 唯一键查询，不存在返回 false（Controller 返回"未收藏"提示）→ DELETE。
     *
     * @param userId 当前登录用户 ID
     * @param dto    收藏请求
     * @return true=取消成功；false=未收藏过
     */
    @Override
    public boolean cancelFavorite(Long userId, FavoriteDTO dto) {
        log.info("取消收藏, userId: {}, targetType: {}, targetId: {}",
                userId, dto.getTargetType(), dto.getTargetId());

        checkTargetType(dto.getTargetType());

        // 幂等：未收藏直接返回 false（Controller 返回"未收藏"提示）
        Favorite exist = getFavorite(userId, dto.getTargetType(), dto.getTargetId());
        if (exist == null) {
            return false;
        }

        removeById(exist.getId());
        log.info("取消收藏成功, favoriteId: {}", exist.getId());
        return true;
    }

    // ==================== 收藏列表 ====================

    /**
     * 收藏列表（分页 + 关键词）
     * <p>
     * 按 targetType 走不同的联表 SQL（展览联 exhibition+gallery，美术馆联 gallery），
     * keyword 模糊匹配标题/名称，pageSize 上限 50。
     *
     * @param userId     当前登录用户 ID
     * @param targetType 收藏类型
     * @param keyword    搜索关键词（可空）
     * @param page       页码
     * @param pageSize   每页条数
     * @return 分页收藏列表
     */
    @Override
    public FavoritePageVO getFavoriteList(Long userId, Integer targetType, String keyword, Integer page, Integer pageSize) {
        log.info("查询收藏列表, userId: {}, targetType: {}, keyword: {}, page: {}, pageSize: {}",
                userId, targetType, keyword, page, pageSize);

        checkTargetType(targetType);

        // 参数兜底：page 默认 1，pageSize 默认 10、下限 1、上限 50
        // （pageSize <= 0 会让 MyBatis-Plus 跳过 count + 分页，导致查全表）
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));

        Page<FavoriteListVO> pageResult;
        if (targetType == TARGET_TYPE_EXHIBITION) {
            pageResult = baseMapper.selectFavoriteExhibitionPage(
                    Page.of(safePage, safeSize), userId, keyword);
        } else {
            pageResult = baseMapper.selectFavoriteGalleryPage(
                    Page.of(safePage, safeSize), userId, keyword);
        }

        return FavoritePageVO.builder()
                .records(pageResult.getRecords())
                .total(pageResult.getTotal())
                .pages(pageResult.getPages())
                .current(pageResult.getCurrent())
                .size(pageResult.getSize())
                .build();
    }

    // ==================== 收藏状态检查 ====================

    /**
     * 检查当前用户是否已收藏目标
     *
     * @param userId     当前登录用户 ID
     * @param targetType 收藏类型
     * @param targetId   收藏目标 ID
     * @return 是否已收藏
     */
    @Override
    public FavoriteCheckVO checkFavorite(Long userId, Integer targetType, Long targetId) {
        log.info("检查收藏状态, userId: {}, targetType: {}, targetId: {}", userId, targetType, targetId);

        checkTargetType(targetType);

        boolean favorited = getFavorite(userId, targetType, targetId) != null;
        return FavoriteCheckVO.builder().isFavorited(favorited).build();
    }

    // ==================== 收藏数量统计 ====================

    /**
     * 收藏数量统计（个人中心展示）
     * <p>
     * 按 target_type 分组各 count 一次，相加得总数。
     *
     * @param userId 当前登录用户 ID
     * @return 展览/美术馆/总数
     */
    @Override
    public FavoriteCountVO countFavorite(Long userId) {
        log.info("统计收藏数量, userId: {}", userId);

        long exhibitionCount = count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, TARGET_TYPE_EXHIBITION));
        long galleryCount = count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, TARGET_TYPE_GALLERY));

        return FavoriteCountVO.builder()
                .exhibitionCount(exhibitionCount)
                .galleryCount(galleryCount)
                .totalCount(exhibitionCount + galleryCount)
                .build();
    }

    // ==================== 私有工具方法 ====================

    /**
     * 校验收藏类型（仅允许 1-展览 2-美术馆）
     *
     * @param targetType 收藏类型
     * @throws AuthException 类型非法
     */
    private void checkTargetType(Integer targetType) {
        if (targetType == null
                || (targetType != TARGET_TYPE_EXHIBITION && targetType != TARGET_TYPE_GALLERY)) {
            throw new AuthException(AuthMessageConstant.FAVORITE_TYPE_INVALID);
        }
    }

    /**
     * 校验收藏目标存在（展览查 exhibition，美术馆查 gallery）
     *
     * @param targetType 收藏类型
     * @param targetId   收藏目标 ID
     * @throws AuthException 目标不存在
     */
    private void checkTargetExist(Integer targetType, Long targetId) {
        boolean exist = targetType == TARGET_TYPE_EXHIBITION
                ? exhibitionMapper.selectById(targetId) != null
                : galleryMapper.selectById(targetId) != null;
        if (!exist) {
            throw new AuthException(AuthMessageConstant.FAVORITE_TARGET_NOT_EXIST);
        }
    }

    /**
     * 按唯一键 (user_id, target_type, target_id) 查询收藏记录
     *
     * @param userId     用户 ID
     * @param targetType 收藏类型
     * @param targetId   收藏目标 ID
     * @return 收藏记录，不存在返回 null
     */
    private Favorite getFavorite(Long userId, Integer targetType, Long targetId) {
        return getOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId));
    }
}
