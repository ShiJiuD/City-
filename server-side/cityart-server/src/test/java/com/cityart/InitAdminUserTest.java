package com.cityart;

import com.cityart.entity.AdminUser;
import com.cityart.mapper.AdminUserMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * 初始化数据测试 —— 往 admin_user 表插入第一条管理员记录
 */
@SpringBootTest
class InitAdminUserTest {

    @Resource
    private AdminUserMapper adminUserMapper;

    @Test
    void insertFirstAdmin() {
        // ===== 1. 用 BCrypt 加密密码 =====
        String rawPassword = "123456";
        String encrypted = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        System.out.println("明文: " + rawPassword);
        System.out.println("密文: " + encrypted);

        // ===== 2. 构建 AdminUser 对象 =====
        AdminUser admin = new AdminUser();
        admin.setPhone("18102213052");
        admin.setPassword(encrypted);
        admin.setName("管理员");
        admin.setStatus(0);                     // 0=启用
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        // ===== 3. 插入数据库 =====
        int rows = adminUserMapper.insert(admin);
        System.out.println("插入结果: " + rows + " 行, 自增ID: " + admin.getId());

        // ===== 4. 验证密码可以正常校验 =====
        AdminUser dbUser = adminUserMapper.selectById(admin.getId());
        boolean check = BCrypt.checkpw(rawPassword, dbUser.getPassword());
        System.out.println("BCrypt 验密: " + (check ? "✅ 通过" : "❌ 失败"));
    }
}
