package com.cityart.interceptor;

import com.cityart.constant.MessageConstant;
import com.cityart.context.AdminContext;
import com.cityart.context.UserContext;
import com.cityart.service.TokenBlacklistService;
import com.cityart.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;  // 非 Controller 请求（如静态资源）直接放行
        }
        // 从请求头中获取 token 参数
        String tokenHeader = request.getHeader("Authorization");
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            log.info("token is null or not start with Bearer");
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"未登录或 token 已过期\"}");
            return false;
        }
        // 从 tokenHeader 中获取 token
        String token = tokenHeader.substring(7);

        // ===== 黑名单校验：已退出的 token 直接拒绝 =====
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.info("token 已被作废（已退出登录）");
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"token 已作废，请重新登录\"}");
            return false;
        }

        // 验证 token 是否有效
        if (!jwtUtil.isValid(token)) {
            log.info("token is not valid");
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"未登录或 token 已过期\"}");
            return false;
        }
        // 从 token 中获取用户角色
        String role = jwtUtil.getRole(token);
        // 设置角色
        if (MessageConstant.USER_ROLE.equals(role)) {
            UserContext.setUserId(jwtUtil.getId(token));
            UserContext.setUserRole(role);
        } else {
            AdminContext.setAdminId(jwtUtil.getId(token));
            AdminContext.setAdminRole(role);
        }

        return true;
    }

    /** 清除上下文 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AdminContext.clear();
        UserContext.clear();
    }

}
