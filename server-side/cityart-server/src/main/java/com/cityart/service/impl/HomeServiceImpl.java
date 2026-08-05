package com.cityart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cityart.entity.Banner;
import com.cityart.entity.Gallery;
import com.cityart.mapper.BannerMapper;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.HomeService;
import com.cityart.vo.BannerVO;
import com.cityart.vo.GalleryVO;
import com.cityart.vo.HomeExhibitionVO;
import com.cityart.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 首页聚合数据 服务实现类
 * </p>
 *
 * @author shijiu
 * @since 2026-07-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HomeServiceImpl implements HomeService {

    private final BannerMapper bannerMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final GalleryMapper galleryMapper;

    @Override
    public HomeVO getHomeData(String city) {
        log.info("获取首页聚合数据, city: {}", city);

        // 1. 查询 Banner，按 sort 降序
        LambdaQueryWrapper<Banner> bannerWrapper = Wrappers.lambdaQuery();
        bannerWrapper.orderByDesc(Banner::getSort);
        List<Banner> banners = bannerMapper.selectList(bannerWrapper);
        List<BannerVO> bannerVOs = banners.stream()
                .map(b -> BeanUtil.copyProperties(b, BannerVO.class))
                .collect(Collectors.toList());
        log.info("Banner 查询完毕，共 {} 条", bannerVOs.size());

        // 2. 查询热门展览（is_hot=1, status=1），联表拿美术馆名称
        List<HomeExhibitionVO> hotExhibitions = exhibitionMapper.selectHotExhibitions();
        log.info("热门展览查询完毕，共 {} 条", hotExhibitions.size());

        // 3. 查询美术馆（status=0 正常营业），可选城市筛选
        LambdaQueryWrapper<Gallery> galleryWrapper = Wrappers.lambdaQuery();
        galleryWrapper.eq(Gallery::getStatus, 0);
        if (StringUtils.hasText(city)) {
            galleryWrapper.eq(Gallery::getCity, city);
        }
        galleryWrapper.orderByDesc(Gallery::getExhibitionCount);
        List<Gallery> galleries = galleryMapper.selectList(galleryWrapper);
        List<GalleryVO> galleryVOs = galleries.stream()
                .map(g -> BeanUtil.copyProperties(g, GalleryVO.class))
                .collect(Collectors.toList());
        log.info("美术馆查询完毕，共 {} 条", galleryVOs.size());

        // 4. 组装返回
        return HomeVO.builder()
                .banners(bannerVOs)
                .hotExhibitions(hotExhibitions)
                .galleries(galleryVOs)
                .build();
    }
}
