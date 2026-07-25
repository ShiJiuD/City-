package com.cityart.controller.user;

import com.cityart.constant.AuthMessageConstant;
import com.cityart.context.UserContext;
import com.cityart.dto.UpdateUserProfileDTO;
import com.cityart.result.Result;
import com.cityart.service.UserService;
import com.cityart.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "C端用户个人信息")
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;

    @Operation(summary = "获取用户个人信息")
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        Long userId = UserContext.getUserId();
        log.info("获取用户个人信息, userId: {}", userId);
        UserProfileVO vo = userService.getProfile(userId);
        return Result.success(vo);
    }

    @Operation(summary = "修改用户个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody @Validated UpdateUserProfileDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("修改用户个人信息, userId: {}, dto: {}", userId, dto);
        userService.updateProfile(userId, dto.getNickname());
        return Result.success(null, AuthMessageConstant.UPDATE_PROFILE_SUCCESS);
    }
}
