package com.cityart.mapper;

import com.cityart.entity.Exhibition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityart.vo.ExhibitionListVO;
import com.cityart.vo.HomeExhibitionVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
     * 查询热门展览（is_hot=1 且当前在展期内，按日期实时判断），联表查美术馆名称
     */
    List<HomeExhibitionVO> selectHotExhibitions();

    /**
     * 按时间段查询展览列表（纯日期判断，不依赖 status）
     * <p>
     * period: current=正在展出（展期内） future=即将展出（未开始） past=往期（已结束）<br>
     * keyword 模糊匹配标题，type 展览类型筛选，city 美术馆所在城市（联表 gallery.city）
     */
    List<ExhibitionListVO> selectExhibitionList(@Param("period") String period,
                                                @Param("keyword") String keyword,
                                                @Param("type") Integer type,
                                                @Param("city") String city);

    /**
     * 原子更新已售票数（sold_count + delta），带库存上限保底
     * <p>
     * {@code total_stock >= sold_count + delta} 确保 MySQL 层面不会超卖。
     * 返回值 = 0 表示库存不足（Redis 预检可能漏过的极端情况）。
     */
    @Update("UPDATE exhibition SET sold_count = sold_count + #{delta} "
            + "WHERE id = #{id} AND total_stock >= sold_count + #{delta}")
    int updateSoldCount(Long id, int delta);
}
