package com.cityart.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cityart.dto.FavoriteDTO;
import com.cityart.entity.Favorite;
import com.cityart.vo.FavoriteCheckVO;
import com.cityart.vo.FavoriteCountVO;
import com.cityart.vo.FavoritePageVO;

/**
 * 收藏模块 服务接口
 *
 * @author shijiu
 * @since 2026-08-11
 */
public interface FavoriteService extends IService<Favorite> {

    /**
     * 添加收藏（幂等）
     *
     * @param userId 当前登录用户 ID
     * @param dto    收藏请求（targetType: 1-展览 2-美术馆, targetId）
     * @return true=新增收藏成功；false=已收藏过（重复收藏，不报错）
     */
    boolean addFavorite(Long userId, FavoriteDTO dto);

    /**
     * 取消收藏（幂等）
     *
     * @param userId 当前登录用户 ID
     * @param dto    收藏请求
     * @return true=取消成功；false=未收藏过（重复取消，不报错）
     */
    boolean cancelFavorite(Long userId, FavoriteDTO dto);

    /**
     * 收藏列表（分页 + 关键词）
     * <p>
     * keyword 模糊匹配：type=1 匹配展览标题，type=2 匹配美术馆名称。
     *
     * @param userId     当前登录用户 ID
     * @param targetType 收藏类型（1-展览 2-美术馆）
     * @param keyword    搜索关键词（可空）
     * @param page       页码（默认 1）
     * @param pageSize   每页条数（默认 10，上限 50）
     * @return 分页收藏列表
     */
    FavoritePageVO getFavoriteList(Long userId, Integer targetType, String keyword, Integer page, Integer pageSize);

    /**
     * 检查收藏状态
     *
     * @param userId     当前登录用户 ID
     * @param targetType 收藏类型（1-展览 2-美术馆）
     * @param targetId   收藏目标 ID
     * @return 是否已收藏
     */
    FavoriteCheckVO checkFavorite(Long userId, Integer targetType, Long targetId);

    /**
     * 收藏数量统计（个人中心展示）
     *
     * @param userId 当前登录用户 ID
     * @return 展览/美术馆/总数三个维度
     */
    FavoriteCountVO countFavorite(Long userId);
}
