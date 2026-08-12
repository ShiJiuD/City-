package com.cityart;

import com.cityart.entity.AdminUser;
import com.cityart.entity.User;
import com.cityart.mapper.AdminUserMapper;
import com.cityart.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * 初始化数据测试 —— 往 admin 表插入第一条管理员记录
 */
@SpringBootTest
class InitUserTest {

    @Resource
    private UserMapper userMapper;

    @Test
    void insertFirstUser() {
        // ===== 1. 用 BCrypt 加密密码 =====
        String rawPassword = "123456";
        String encrypted = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        System.out.println("明文: " + rawPassword);
        System.out.println("密文: " + encrypted);

        // ===== 2. 构建 AdminUser 对象 =====
        User user = new User();
        user.setPhone("18102213052");
        user.setPassword(encrypted);
        user.setNickname("拾酒");
        user.setStatus(0);                     // 0=启用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // ===== 3. 插入数据库 =====
        int rows = userMapper.insert(user);
        System.out.println("插入结果: " + rows + " 行, 自增ID: " + user.getId());

        // ===== 4. 验证密码可以正常校验 =====
        User dbUser = userMapper.selectById(user.getId());
        boolean check = BCrypt.checkpw(rawPassword, dbUser.getPassword());
        System.out.println("BCrypt 验密: " + (check ? "✅ 通过" : "❌ 失败"));
    }
}
