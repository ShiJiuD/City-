package com.cityart.controller.admin;

import com.cityart.constant.AuthMessageConstant;
import com.cityart.context.AdminContext;
import com.cityart.dto.UpdateAdminProfileDTO;
import com.cityart.result.Result;
import com.cityart.service.AdminUserService;
import com.cityart.vo.AdminProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "B端管理员")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminProfileController {

    private final AdminUserService adminUserService;

    @Operation(summary = "获取管理员个人信息")
    @GetMapping("/profile")
    public Result<AdminProfileVO> getProfile() {
        Long adminId = AdminContext.getAdminId();
        log.info("获取管理员个人信息, adminId: {}", adminId);
        AdminProfileVO vo = adminUserService.getProfile(adminId);
        return Result.success(vo);
    }

    @Operation(summary = "修改管理员个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody @Validated UpdateAdminProfileDTO dto) {
        Long adminId = AdminContext.getAdminId();
        log.info("修改管理员个人信息, adminId: {}, dto: {}", adminId, dto);
        adminUserService.updateProfile(adminId, dto.getName());
        return Result.success(null, AuthMessageConstant.UPDATE_PROFILE_SUCCESS);
    }
}
