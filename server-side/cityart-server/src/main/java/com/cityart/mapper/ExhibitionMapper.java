package com.cityart.mapper;

import com.cityart.entity.Exhibition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityart.vo.HomeExhibitionVO;

import java.util.List;

/**
 * <p>
 * 展览信息表 Mapper 接口
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
public interface ExhibitionMapper extends BaseMapper<Exhibition> {

    /**
     * 查询热门展览（is_hot=1, status=1），联表查美术馆名称
     *
     * @return 热门展览列表（含美术馆名称）
     */
    List<HomeExhibitionVO> selectHotExhibitions();

}
