package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页热门展览 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeExhibitionVO {

    private Long id;
    private String posterImage;
    private String title;
    private String subtitle;
    private String galleryName;
    private Integer type;
}
