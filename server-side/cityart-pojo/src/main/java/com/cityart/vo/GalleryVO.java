package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 美术馆 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryVO {

    private Long id;
    private String name;
    private String coverImage;
    private String address;
    private Integer exhibitionCount;
    private Integer type;
}
