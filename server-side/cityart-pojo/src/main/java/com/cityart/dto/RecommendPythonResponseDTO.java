package com.cityart.dto;

import lombok.Data;

import java.util.List;

/**
 * Python → Java 推荐响应体（07 文档 6.2）
 * <p>
 * data 为排好序的展览 ID 数组，按推荐得分从高到低排列。
 * code 非 1 / data 为 null / 空数组 → Java 侧降级返回热门展览。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Data
public class RecommendPythonResponseDTO {

    /** 1=成功，0=失败 */
    private Integer code;

    /** 提示文案 */
    private String msg;

    /** 排好序的展览 ID 数组，按推荐得分从高到低排列 */
    private List<Long> data;
}
