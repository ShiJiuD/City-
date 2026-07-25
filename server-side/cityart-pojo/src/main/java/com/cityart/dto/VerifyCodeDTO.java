package com.cityart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 忘记密码-校验验证码请求体
 */
@Data
public class VerifyCodeDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(user|admin)$", message = "角色参数错误，仅支持 user 或 admin")
    private String role;
}
