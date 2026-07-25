package com.cityart.service;

import com.cityart.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cityart.vo.LoginVO;
import com.cityart.vo.UserProfileVO;

/**
 * <p>
 * C端普通用户表 服务类
 * </p>
 *
 * @author
 * @since 2026-07-24
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册（成功自动登录返回 Token）
     *
     * @param phone    手机号
     * @param password 密码
     * @return 用户信息 + Token
     */
    LoginVO register(String phone, String password);

    /**
     * 用户登录
     *
     * @param phone    手机号
     * @param password 密码
     * @return 用户信息 + Token
     */
    LoginVO login(String phone, String password);

    /**
     * 获取用户个人信息（页面刷新用）
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 修改用户昵称
     *
     * @param userId   用户ID
     * @param nickname 新昵称
     */
    void updateProfile(Long userId, String nickname);
}
