package com.cityart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cityart.constant.AuthMessageConstant;
import com.cityart.entity.Exhibition;
import com.cityart.entity.ExhibitionWork;
import com.cityart.entity.Gallery;
import com.cityart.exception.AuthException;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.ExhibitionWorkMapper;
import com.cityart.mapper.GalleryMapper;
import com.cityart.service.DetailService;
import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.ExhibitionWorkVO;
import com.cityart.vo.GalleryDetailVO;
import com.cityart.vo.GalleryExhibitionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 详情模块 服务实现类
 * <p>
 * 展馆详情：查 gallery 基本信息，再按 galleryId 查展览，按日期拆分正在展出/往期；
 * 展览详情：查 exhibition，联表取美术馆名称/地址，再查该展览的作品集。
 * 免费展览 price 返回 0（与列表接口 COALESCE 语义一致）。
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetailServiceImpl implements DetailService {

    private final GalleryMapper galleryMapper;
    private final ExhibitionMapper exhibitionMapper;
    private final ExhibitionWorkMapper exhibitionWorkMapper;

    @Override
    public GalleryDetailVO getGalleryDetail(Long id) {
        log.info("查询展馆详情, id: {}", id);

        Gallery gallery = galleryMapper.selectById(id);
        if (gallery == null) {
            throw new AuthException(AuthMessageConstant.GALLERY_NOT_EXIST);
        }

        // 该馆下所有展览，按排序权重降序；未开始的展览不展示
        LambdaQueryWrapper<Exhibition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Exhibition::getGalleryId, id)
                .orderByDesc(Exhibition::getSortOrder);

        List<Exhibition> exhibitions = exhibitionMapper.selectList(wrapper);

        LocalDate today = LocalDate.now();
        List<GalleryExhibitionVO> current = new ArrayList<>();
        List<GalleryExhibitionVO> past = new ArrayList<>();
        for (Exhibition e : exhibitions) {
            if (e.getStartDate().isAfter(today)) {
                continue; // 未开始，不展示
            }
            GalleryExhibitionVO vo = BeanUtil.copyProperties(e, GalleryExhibitionVO.class);
            vo.setPrice(e.getPrice() != null ? e.getPrice() : BigDecimal.ZERO);
            if (e.getEndDate().isBefore(today)) {
                past.add(vo);
            } else {
                current.add(vo);
            }
        }
        log.info("展馆详情查询完毕, id: {}, 当前展出 {} 条, 往期 {} 条", id, current.size(), past.size());

        return GalleryDetailVO.builder()
                .id(gallery.getId())
                .name(gallery.getName())
                .coverImage(gallery.getCoverImage())
                .address(gallery.getAddress())
                .intro(gallery.getIntro())
                .exhibitionCount(gallery.getExhibitionCount())
                .currentExhibitions(current)
                .pastExhibitions(past)
                .build();
    }

    @Override
    public ExhibitionDetailVO getExhibitionDetail(Long id) {
        log.info("查询展览详情, id: {}", id);

        Exhibition exhibition = exhibitionMapper.selectById(id);
        if (exhibition == null) {
            throw new AuthException(AuthMessageConstant.EXHIBITION_NOT_EXIST);
        }

        // 联表取美术馆名称/地址
        Gallery gallery = galleryMapper.selectById(exhibition.getGalleryId());

        // 作品集按排序权重降序
        LambdaQueryWrapper<ExhibitionWork> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExhibitionWork::getExhibitionId, id)
                .orderByDesc(ExhibitionWork::getSortOrder);
        List<ExhibitionWork> works = exhibitionWorkMapper.selectList(wrapper);
        List<ExhibitionWorkVO> workVOs = works.stream()
                .map(w -> BeanUtil.copyProperties(w, ExhibitionWorkVO.class))
                .collect(Collectors.toList());
        log.info("展览详情查询完毕, id: {}, 作品 {} 条", id, workVOs.size());

        return ExhibitionDetailVO.builder()
                .id(exhibition.getId())
                .title(exhibition.getTitle())
                .subtitle(exhibition.getSubtitle())
                .posterImage(exhibition.getPosterImage())
                .galleryName(gallery != null ? gallery.getName() : null)
                .galleryAddress(gallery != null ? gallery.getAddress() : null)
                .startDate(exhibition.getStartDate())
                .endDate(exhibition.getEndDate())
                .price(exhibition.getPrice() != null ? exhibition.getPrice() : BigDecimal.ZERO)
                .description(exhibition.getDescription())
                .works(workVOs)
                .build();
    }
}
