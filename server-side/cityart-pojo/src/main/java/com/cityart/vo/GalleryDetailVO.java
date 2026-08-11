package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 展馆详情 VO
 * <p>
 * 包含展馆基本信息 + 该馆下展览列表（正在展出、往期）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryDetailVO {

    private Long id;
    private String name;
    private String coverImage;
    private String address;
    private String intro;
    private Integer exhibitionCount;
    private List<GalleryExhibitionVO> currentExhibitions;
    private List<GalleryExhibitionVO> pastExhibitions;
}
