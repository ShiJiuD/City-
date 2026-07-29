package com.cityart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cityart.entity.Gallery;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.GalleryService;
import com.cityart.vo.GalleryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 美术馆信息表 服务实现类
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
@Service
@Slf4j
public class GalleryServiceImpl extends ServiceImpl<GalleryMapper, Gallery> implements GalleryService {

    @Override
    public List<GalleryVO> getGalleryList(String city, String keyword) {
        log.info("查询美术馆列表, city: {}, keyword: {}", city, keyword);

        LambdaQueryWrapper<Gallery> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Gallery::getStatus, 0);

        // 城市筛选
        if (StringUtils.hasText(city)) {
            wrapper.eq(Gallery::getCity, city);
        }

        // 名称模糊搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Gallery::getName, keyword);
        }

        wrapper.orderByDesc(Gallery::getExhibitionCount);

        List<Gallery> galleries = baseMapper.selectList(wrapper);
        log.info("美术馆列表查询完毕，共 {} 条", galleries.size());

        return galleries.stream()
                .map(g -> BeanUtil.copyProperties(g, GalleryVO.class))
                .collect(Collectors.toList());
    }
}
