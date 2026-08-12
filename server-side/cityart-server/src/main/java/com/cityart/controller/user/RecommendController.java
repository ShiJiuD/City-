package com.cityart.controller.user;

import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.result.Result;
import com.cityart.service.RecommendService;
import com.cityart.service.TokenBlacklistService;
import com.cityart.utils.JwtUtil;
import com.cityart.vo.RecommendExhibitionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页"艺览智荐"推荐板块
 * <p>
 * 🔓 游客可访问（已加入 JWT 白名单，拦截器不执行此路径）：
 * 登录用户返回个性化推荐，未登录/无效 token 按游客返回热门降级。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Tag(name = "C端推荐模块")
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class RecommendController {

    private final RecommendService recommendService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 获取个性化推荐展览列表（07 文档第五章）
     *
     * @param limit      返回推荐数量上限，默认 4
     * @param authHeader 可选 Authorization 头；拦截器不处理此路径，需手动解析 token
     */
    @Operation(summary = "获取个性化推荐展览列表")
    @GetMapping("/recommend")
    public Result<List<RecommendExhibitionVO>> recommend(
            @RequestParam(required = false) String limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = resolveUserId(authHeader);
        List<RecommendExhibitionVO> list = recommendService.getRecommendations(userId, parseLimit(limit));
        return Result.success(list, AuthMessageConstant.RECOMMEND_SUCCESS);
    }

    /**
     * limit 解析：null/空/非数字返回 null，由 service 兜底为默认 4（公开接口，非法入参不得抛 500）
     */
    private Integer parseLimit(String limit) {
        if (limit == null || limit.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(limit);
        } catch (NumberFormatException e) {
            log.warn("推荐接口 limit 参数非法: {}, 按默认值处理", limit);
            return null;
        }
    }

    /**
     * 手动解析可选 token（07 文档：userId 从可选 Token 中解析，未登录不组装画像走热门降级）
     * <p>
     * 无头 / 非 Bearer / 无效过期 / ADMIN 角色 token → 均按游客处理（返回 null）。
     */
    private Long resolveUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        // 与 JwtInterceptor 语义一致：已退出登录（token 被拉黑）的 token 按游客处理，不做个性化
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.info("推荐接口 token 已作废（已退出登录）, 按游客处理");
            return null;
        }
        if (!jwtUtil.isValid(token)) {
            log.info("推荐接口 token 无效或已过期, 按游客处理");
            return null;
        }
        if (!MessageConstant.USER_ROLE.equals(jwtUtil.getRole(token))) {
            log.info("推荐接口收到非 C 端角色 token, 按游客处理");
            return null;
        }
        return jwtUtil.getId(token);
    }
}
