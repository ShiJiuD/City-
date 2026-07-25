package com.cityart.service;

import com.cityart.entity.AdminUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cityart.vo.AdminProfileVO;
import com.cityart.vo.LoginVO;

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
     * @return 管理员信息 + Token
     */
    LoginVO login(String phone, String password);

    /**
     * 获取管理员个人信息（页面刷新用）
     *
     * @param adminId 管理员ID
     * @return 管理员信息
     */
    AdminProfileVO getProfile(Long adminId);

    /**
     * 修改管理员姓名
     *
     * @param adminId 管理员ID
     * @param name    新姓名
     */
    void updateProfile(Long adminId, String name);
}
