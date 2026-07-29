package com.cityart.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.entity.AdminUser;
import com.cityart.exception.AuthException;
import com.cityart.mapper.AdminUserMapper;
import com.cityart.service.AdminUserService;
import com.cityart.utils.JwtUtil;
import com.cityart.vo.AdminProfileVO;
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
        log.info("管理员登录, 手机号: {}", phone);

        // 1. 按手机号查询
        LambdaQueryWrapper<AdminUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AdminUser::getPhone, phone);
        AdminUser adminUser = getOne(wrapper);

        // 2. 统一返回"账号或密码错误"，不区分手机号不存在 / 密码错误
        if (adminUser == null) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 3. 校验账号状态
        if (adminUser.getStatus() != null && adminUser.getStatus() == 1) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_DISABLED);
        }

        // 4. BCrypt 校验密码
        if (!BCrypt.checkpw(password, adminUser.getPassword())) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 5. 生成 JWT Token
        String token = jwtUtil.generateToken(adminUser.getId(), MessageConstant.ADMIN_ROLE);

        // 6. 构造返回 VO
        LoginVO vo = BeanUtil.copyProperties(adminUser, LoginVO.class);
        vo.setToken(token);
        return vo;
    }

    @Override
    public AdminProfileVO getProfile(Long adminId) {
        log.info("获取管理员个人信息, adminId: {}", adminId);
        AdminUser adminUser = getById(adminId);
        if (adminUser == null) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_NOT_EXIST);
        }
        return BeanUtil.copyProperties(adminUser, AdminProfileVO.class);
    }

    @Override
    public void updateProfile(Long adminId, String name) {
        log.info("修改管理员姓名, adminId: {}, name: {}", adminId, name);
        AdminUser adminUser = getById(adminId);
        if (adminUser == null) {
            throw new AuthException(AuthMessageConstant.ACCOUNT_NOT_EXIST);
        }
        adminUser.setName(name);
        updateById(adminUser);
    }
}
