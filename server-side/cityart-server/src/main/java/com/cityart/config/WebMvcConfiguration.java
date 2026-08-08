package com.cityart.config;

import com.cityart.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;


    // ========== CORS 跨域 ==========

    /**
     * 允许前端跨域请求
     *
     * 前端 Vite 开发服务器：http://localhost:9528
     * 后端 Spring Boot：http://localhost:8080
     * → 不同端口 = 跨域，浏览器会拦截不发送
     *
     * addCorsMappings 告诉浏览器：这个后端允许你跨域访问
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                        // 对所有接口生效
                .allowedOriginPatterns("*")               // 允许所有来源（开发环境）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的请求方法
                .allowedHeaders("*")                      // 允许所有请求头
                .allowCredentials(true)                   // 允许携带 Cookie / Authorization
                .maxAge(3600);                            // 预检请求缓存 1 小时
    }

    // ========== 注册拦截器 ==========

    /**
     * 注册 JWT 认证拦截器
     *
     * .addPathPatterns("/api/admin/**")          → 拦截所有管理员接口
     * .excludePathPatterns("/api/admin/login")    → 登录接口不拦截（没登录哪来 token）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("注册 JWT 拦截器...");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")          // 拦截所有 /api/admin/ 开头的请求
                .excludePathPatterns("/api/app/login",
                        "/api/app/register",
                        "/api/app/home",
                        "/api/app/galleries",
                        "/api/app/galleries/page",
                        "/api/app/exhibitions/current",
                        "/api/app/exhibitions/future",
                        "/api/app/exhibitions/past",
                        "/api/admin/login",
                        "/api/admin/register",
                        "/api/send-code",
                        "/api/verify-code",
                        "/api/reset-password"
                );   // 登录接口 + 首页展览浏览接口放行
    }


//    @Override
//    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        // 创消息转换器
//        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//        // 设置自定义的 ObjectMapper
//        converter.setObjectMapper(new JacksonObjectMapper());
//        // 注册消息转换器，优先级最高
//        converters.add(2, converter);
//    }
}
