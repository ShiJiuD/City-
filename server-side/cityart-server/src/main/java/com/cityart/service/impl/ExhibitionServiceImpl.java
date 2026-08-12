package com.cityart.service.impl;

import com.cityart.entity.Exhibition;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.service.ExhibitionService;
import com.cityart.vo.ExhibitionListVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 展览信息表 服务实现类
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
@Service
@Slf4j
public class ExhibitionServiceImpl extends ServiceImpl<ExhibitionMapper, Exhibition> implements ExhibitionService {

    @Override
    public List<ExhibitionListVO> getExhibitionList(String period, String keyword, Integer type, String city) {
        log.info("查询展览列表, period: {}, keyword: {}, type: {}, city: {}", period, keyword, type, city);
        return baseMapper.selectExhibitionList(period, keyword, type, city);
    }

}
