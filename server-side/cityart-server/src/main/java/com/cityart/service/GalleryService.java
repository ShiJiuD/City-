package com.cityart.service;

import com.cityart.entity.Gallery;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cityart.vo.GalleryPageVO;
import com.cityart.vo.GalleryVO;

import java.util.List;

/**
 * <p>
 * 美术馆信息表 服务类
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
public interface GalleryService extends IService<Gallery> {

    /**
     * 查询美术馆列表，支持城市筛选和名称模糊搜索
     *
     * @param city    城市筛选（可选）
     * @param keyword 名称模糊搜索（可选）
     * @return 美术馆列表
     */
    List<GalleryVO> getGalleryList(String city, String keyword);

    /**
     * 分页查询美术馆列表，支持关键词模糊搜索、类型、城市筛选
     * <p>
     * 仅返回营业中（status=0）的美术馆，按 exhibition_count 降序。
     *
     * @param pageNum  当前页码，从 1 开始
     * @param pageSize 每页条数
     * @param keyword  名称模糊搜索（可选）
     * @param type     美术馆类型筛选（可选）
     * @param city     城市筛选（可选）
     * @return 分页结果
     */
    GalleryPageVO getGalleryPage(Integer pageNum, Integer pageSize, String keyword, Integer type, String city);
}
