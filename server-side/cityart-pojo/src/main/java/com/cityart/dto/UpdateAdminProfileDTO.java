package com.cityart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * B端管理员修改个人信息请求体（仅姓名）
 */
@Data
public class UpdateAdminProfileDTO {

    @NotBlank(message = "姓名不能为空")
    private String name;
}
