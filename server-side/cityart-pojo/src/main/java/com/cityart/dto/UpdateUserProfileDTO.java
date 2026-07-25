package com.cityart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * C端用户修改个人信息请求体（仅昵称）
 */
@Data
public class UpdateUserProfileDTO {

    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
