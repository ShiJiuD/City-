package com.cityart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.entity.User;
import com.cityart.exception.AuthException;
import com.cityart.mapper.UserMapper;
import com.cityart.service.UserService;
import com.cityart.utils.JwtUtil;
import com.cityart.vo.LoginVO;
import com.cityart.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * <p>
 * C端普通用户表 服务实现类
 * </p>
 *
 * @author
 * @since 2026-07-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;

    @Override
    public LoginVO register(String phone, String password) {
        log.info("用户注册, 手机号: {}", phone);

        // 1. 校验手机号是否已注册
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getPhone, phone);
        User existUser = getOne(wrapper);
        if (existUser != null) {
            throw new AuthException(AuthMessageConstant.PHONE_ALREADY_REGISTER);
        }

        // 2. BCrypt 加密密码
        String encodedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));

        // 3. 构造用户对象（昵称默认取手机号脱敏）
        User user = new User();
        user.setPhone(phone);
        user.setPassword(encodedPassword);
        user.setNickname(maskPhone(phone));
        user.setStatus(0);
        save(user);

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), MessageConstant.USER_ROLE);

        // 5. 构造返回 VO
        LoginVO vo = LoginVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .token(token)
                .build();
        return vo;
    }

    @Override
    public LoginVO login(String phone, String password) {
        log.info("用户登录, 手机号: {}", phone);

        // 1. 按手机号查询
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getPhone, phone);
        User user = getOne(wrapper);

        // 2. 统一返回"账号或密码错误"，不区分手机号不存在 / 密码错误
        if (user == null) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 3. 校验账号状态
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_DISABLED);
        }

        // 4. BCrypt 校验密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 5. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), MessageConstant.USER_ROLE);

        // 6. 构造返回 VO
        LoginVO vo = LoginVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .token(token)
                .build();
        return vo;
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        log.info("获取用户个人信息, userId: {}", userId);
        User user = getById(userId);
        if (user == null) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_NOT_EXIST);
        }
        return BeanUtil.copyProperties(user, UserProfileVO.class);
    }

    @Override
    public void updateProfile(Long userId, String nickname) {
        log.info("修改用户昵称, userId: {}, nickname: {}", userId, nickname);
        User user = getById(userId);
        if (user == null) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_NOT_EXIST);
        }
        user.setNickname(nickname);
        updateById(user);
    }

    /**
     * 手机号脱敏：13800138000 → 138****8000
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
