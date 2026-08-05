package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单分页 VO
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPageVO {

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Long pages;

    /** 当前页码 */
    private Long current;

    /** 每页条数 */
    private Long size;

    /** 当前页订单列表 */
    private List<OrderVO> records;
}
