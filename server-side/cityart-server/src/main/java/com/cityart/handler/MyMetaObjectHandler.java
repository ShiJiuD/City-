package com.cityart.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * <p>
 * 配合实体类字段上的 @TableField(fill = FieldFill.XXX) 注解，
 * 在执行 INSERT / UPDATE 时自动填充 createTime 和 updateTime。
 * </p>
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * INSERT 时自动触发 —— 给标记了 FieldFill.INSERT 或 FieldFill.INSERT_UPDATE 的字段赋值
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 如果实体没有手动设值，才自动填充（strict 模式：有值就不覆盖）
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /**
     * UPDATE 时自动触发 —— 给标记了 FieldFill.UPDATE 或 FieldFill.INSERT_UPDATE 的字段赋值
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 每次更新都刷新 updateTime
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}
