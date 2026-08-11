package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 展览详情 VO
 * <p>
 * 包含展览基本信息、所属美术馆名称/地址、作品集列表。
 * price 未设置时返回 0（免费）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionDetailVO {

    private Long id;
    private String title;
    private String subtitle;
    private String posterImage;
    private String galleryName;
    private String galleryAddress;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal price;
    private String description;
    private List<ExhibitionWorkVO> works;
}
