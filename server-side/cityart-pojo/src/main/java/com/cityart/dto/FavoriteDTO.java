package com.cityart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加/取消收藏请求体
 * <p>
 * targetType 仅允许 1-展览 / 2-美术馆，合法性在 service 统一校验
 * （GET 接口无 DTO，需与 POST/DELETE 校验逻辑一致）。
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Data
public class FavoriteDTO {

    /** 收藏类型：1-展览 2-美术馆 */
    @NotNull(message = "收藏类型不能为空")
    private Integer targetType;

    /** 收藏目标ID（展览ID或美术馆ID） */
    @NotNull(message = "收藏目标ID不能为空")
    private Long targetId;
}
