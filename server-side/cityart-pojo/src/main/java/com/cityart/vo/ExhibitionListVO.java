package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 展览列表 VO（正在展出 / 即将展出 / 往期）
 * <p>
 * 按日期实时判断时间段，不依赖 status；price 未设置时返回 0（免费）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionListVO {

    private Long id;
    private String posterImage;
    private String title;
    private String subtitle;
    private String galleryName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer type;
    private BigDecimal price;
}
