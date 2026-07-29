package com.cityart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置
 *
 * 启动后访问：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ciTY Art 接口文档")
                        .description("美术馆管理系统 API")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("ciTY Art")
                                .email("admin@cityart.com")));
    }
}
