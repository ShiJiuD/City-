package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 展览作品集 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionWorkVO {

    private Long id;
    private String title;
    private String artist;
    private String image;
    private String description;
}
