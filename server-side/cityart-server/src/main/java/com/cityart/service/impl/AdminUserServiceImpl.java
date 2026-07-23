package com.cityart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.entity.AdminUser;
import com.cityart.exception.AuthException;
import com.cityart.mapper.AdminUserMapper;
import com.cityart.service.AdminUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cityart.utils.JwtUtil;
import com.cityart.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * <p>
 * B端美术馆管理员表 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-07-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {

    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(String phone, String password) {
        log.info("卖家登录, 手机号: {}", phone);
        LambdaQueryWrapper<AdminUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AdminUser::getPhone, phone);
        AdminUser adminUser = getOne(wrapper);
        if (adminUser == null) {
            throw new AuthException(AuthMessageConstant.PHONE_NOT_EXIST);
        };
        if (!BCrypt.checkpw(password, adminUser.getPassword())) {
            throw new AuthException(AuthMessageConstant.PASSWORD_ERROR);
        };
        String token = jwtUtil.generateToken(adminUser.getId(), MessageConstant.ADMIN_ROLE);
        LoginVO vo = BeanUtil.copyProperties(adminUser, LoginVO.class);
        vo.setToken(token);
        return vo;
    }
}
