package com.cityart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.constant.RedisConstant;
import com.cityart.dto.ResetPasswordDTO;
import com.cityart.dto.SendCodeDTO;
import com.cityart.dto.VerifyCodeDTO;
import com.cityart.entity.User;
import com.cityart.entity.AdminUser;
import com.cityart.exception.AuthException;
import com.cityart.result.Result;
import com.cityart.service.AdminUserService;
import com.cityart.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Tag(name = "忘记密码")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CommonController {

    private final UserService userService;
    private final AdminUserService adminUserService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 测试环境验证码常量
     */
    private static final String MOCK_VERIFY_CODE = "666666";

    @Operation(summary = "忘记密码-发送验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody @Validated SendCodeDTO dto) {
        log.info("发送验证码请求: phone={}, role={}", dto.getPhone(), dto.getRole());
        checkPhoneExists(dto.getPhone(), dto.getRole());
        // 测试环境不真实发送，直接返回成功
        return Result.success(null, AuthMessageConstant.CODE_SENT);
    }

    @Operation(summary = "忘记密码-校验验证码")
    @PostMapping("/verify-code")
    public Result<Void> verifyCode(@RequestBody @Validated VerifyCodeDTO dto) {
        log.info("校验验证码请求: phone={}, role={}", dto.getPhone(), dto.getRole());

        // 1. 校验验证码是否为 666666
        if (!MOCK_VERIFY_CODE.equals(dto.getCode())) {
            throw new AuthException(AuthMessageConstant.CODE_ERROR);
        }

        // 2. 校验手机号在对应表中存在（验证码场景用"当前账号不存在"）
        checkPhoneExists(dto.getPhone(), dto.getRole());

        // 3. 校验通过 → 写入 Redis 一次性凭证（5 分钟有效），
        //    reset-password 必须持有该凭证才能改密，防止绕过验证码直接重置
        stringRedisTemplate.opsForValue().set(
                RedisConstant.KEY_VERIFY_CODE + dto.getPhone(), "1",
                RedisConstant.CODE_TTL, TimeUnit.MILLISECONDS);
        log.info("验证码校验通过，已写入重置凭证, phone={}", dto.getPhone());

        return Result.success(null, AuthMessageConstant.VERIFY_SUCCESS);
    }

    @Operation(summary = "忘记密码-重置密码")
    @PutMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody @Validated ResetPasswordDTO dto) {
        log.info("重置密码请求: phone={}, role={}", dto.getPhone(), dto.getRole());

        // 0. 校验验证码凭证：必须先通过 /api/verify-code 且 5 分钟内有效，
        //    否则任何人知道手机号即可绕过验证码直接改密（接管账号）
        String verified = stringRedisTemplate.opsForValue().get(RedisConstant.KEY_VERIFY_CODE + dto.getPhone());
        if (verified == null) {
            throw new AuthException(AuthMessageConstant.VERIFY_CODE_REQUIRED);
        }

        // 1. 再次校验手机号在对应表中存在
        checkPhoneExists(dto.getPhone(), dto.getRole());

        // 2. BCrypt 加密新密码
        String encodedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10));

        // 3. 按 role 更新对应表
        if (MessageConstant.USER_ROLE.equals(dto.getRole())) {
            LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(User::getPhone, dto.getPhone());
            User user = userService.getOne(wrapper);
            user.setPassword(encodedPassword);
            userService.updateById(user);
        } else {
            LambdaQueryWrapper<AdminUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(AdminUser::getPhone, dto.getPhone());
            AdminUser adminUser = adminUserService.getOne(wrapper);
            adminUser.setPassword(encodedPassword);
            adminUserService.updateById(adminUser);
        }

        // 4. 凭证一次性：改密成功后立即删除，防止凭证复用
        stringRedisTemplate.delete(RedisConstant.KEY_VERIFY_CODE + dto.getPhone());

        return Result.success(null, AuthMessageConstant.PASSWORD_RESET_SUCCESS);
    }

    /**
     * 按 role 校验手机号在对应表中是否存在
     * @param phone     手机号
     * @param role      角色 user / admin
     */
    private void checkPhoneExists(String phone, String role) {
        if (MessageConstant.USER_ROLE.equals(role)) {
            LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(User::getPhone, phone);
            if (userService.getOne(wrapper) == null) {
                throw new AuthException(AuthMessageConstant.PHONE_NOT_EXIST);
            }
        } else {
            LambdaQueryWrapper<AdminUser> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(AdminUser::getPhone, phone);
            if (adminUserService.getOne(wrapper) == null) {
                throw new AuthException(AuthMessageConstant.ACCOUNT_NOT_EXIST);
            }
        }
    }
}
