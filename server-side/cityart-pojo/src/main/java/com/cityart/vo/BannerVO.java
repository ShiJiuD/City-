package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Banner VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerVO {

    private Long id;
    private String imageUrl;
    private String title;
}
