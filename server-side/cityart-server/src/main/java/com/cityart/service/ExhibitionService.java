package com.cityart.service;

import com.cityart.entity.Exhibition;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cityart.vo.ExhibitionListVO;

import java.util.List;

/**
 * <p>
 * 展览信息表 服务类
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
public interface ExhibitionService extends IService<Exhibition> {

    /**
     * 按时间段查询展览列表（纯日期判断，不依赖 status）
     *
     * @param period  current=正在展出 future=即将展出 past=往期
     * @param keyword 展览标题模糊搜索（可空）
     * @param type    展览类型筛选（可空）
     * @param city    城市筛选，关联美术馆表（可空）
     * @return 展览列表 VO
     */
    List<ExhibitionListVO> getExhibitionList(String period, String keyword, Integer type, String city);

}
