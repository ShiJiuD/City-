package com.cityart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cityart")
@MapperScan("com.cityart.mapper")  // 扫描 mapper 接口，自动创建实现类 mapper接口可以不写@Mapper注解
public class CityArtApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityArtApplication.class, args);
    }
}
