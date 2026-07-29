package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回数据（user / admin 共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private Long id;
    private String phone;
    private String name;        // 管理员姓名
    private String nickname;    // 用户昵称
    private String token;
}
