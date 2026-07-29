package com.cityart.controller.user;

import com.cityart.constant.AuthMessageConstant;
import com.cityart.dto.LoginDTO;
import com.cityart.dto.RegisterDTO;
import com.cityart.result.Result;
import com.cityart.service.UserService;
import com.cityart.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "C端用户登录")
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class UserLoginController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Validated RegisterDTO dto) {
        log.info("用户注册请求: {}", dto);
        LoginVO vo = userService.register(dto.getPhone(), dto.getPassword());
        return Result.success(vo, AuthMessageConstant.REGISTER_SUCCESS);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated LoginDTO dto) {
        log.info("用户登录请求: {}", dto);
        LoginVO vo = userService.login(dto.getPhone(), dto.getPassword());
        return Result.success(vo, AuthMessageConstant.LOGIN_SUCCESS);
    }
}
