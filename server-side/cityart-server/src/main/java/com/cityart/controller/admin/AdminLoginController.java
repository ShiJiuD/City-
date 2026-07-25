package com.cityart.controller.admin;

import com.cityart.constant.AuthMessageConstant;
import com.cityart.dto.LoginDTO;
import com.cityart.result.Result;
import com.cityart.service.AdminUserService;
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

@Tag(name = "B端管理员登录")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminLoginController {

    private final AdminUserService adminUserService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated LoginDTO dto) {
        log.info("管理员登录请求: {}", dto);
        LoginVO loginVO = adminUserService.login(dto.getPhone(), dto.getPassword());
        return Result.success(loginVO, AuthMessageConstant.LOGIN_SUCCESS);
    }
}
