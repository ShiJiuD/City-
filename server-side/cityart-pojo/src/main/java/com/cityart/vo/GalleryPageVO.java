package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 美术馆分页 VO
 *
 * @author shijiu
 * @since 2026-08-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryPageVO {

    /** 当前页数据列表 */
    private List<GalleryVO> records;

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Long pages;

    /** 当前页码 */
    private Long current;

    /** 每页条数 */
    private Long size;
}
