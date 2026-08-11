package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 展馆详情中的展览项 VO（正在展出 / 往期）
 * <p>
 * price 未设置时返回 0（免费）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryExhibitionVO {

    private Long id;
    private String title;
    private String posterImage;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal price;
}
