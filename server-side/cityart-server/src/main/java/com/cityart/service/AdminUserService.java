package com.cityart.service;

import com.cityart.entity.AdminUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cityart.vo.LoginVO;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * B端美术馆管理员表 服务类
 * </p>
 *
 * @author 
 * @since 2026-07-24
 */
public interface AdminUserService extends IService<AdminUser> {

    /**
     * 管理员登录
     *
     * @param phone    手机号
     * @param password 密码
     * @return 登录成功返回管理员对象，失败返回 null
     */
    LoginVO login(@NotBlank(message = "手机号不能为空") String phone, @NotBlank(message = "密码不能为空") String password);
}
