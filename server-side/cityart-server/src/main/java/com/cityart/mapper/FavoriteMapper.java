package com.cityart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityart.entity.Favorite;
import com.cityart.vo.FavoriteListVO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 用户收藏表 Mapper 接口
 * </p>
 *
 * @author shijiu
 * @since 2026-08-11
 */
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 展览收藏分页查询（联表 exhibition + gallery 补标题/美术馆名称/海报/展期/票价）
     * <p>
     * keyword 模糊匹配展览标题；按收藏时间倒序。
     */
    Page<FavoriteListVO> selectFavoriteExhibitionPage(Page<?> page,
                                                      @Param("userId") Long userId,
                                                      @Param("keyword") String keyword);

    /**
     * 美术馆收藏分页查询（联表 gallery 补名称/封面/地址/在展数量）
     * <p>
     * keyword 模糊匹配美术馆名称；按收藏时间倒序。
     */
    Page<FavoriteListVO> selectFavoriteGalleryPage(Page<?> page,
                                                   @Param("userId") Long userId,
                                                   @Param("keyword") String keyword);
}
