-- 用户收藏表（展览 + 美术馆统一收藏，target_type 区分）
-- 表结构依据《收藏数据库设计文档》
CREATE TABLE IF NOT EXISTS `favorite` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT      NOT NULL                COMMENT '用户ID，关联 user.id',
    `target_type` TINYINT     NOT NULL                COMMENT '收藏类型：1-展览 2-美术馆',
    `target_id`   BIGINT      NOT NULL                COMMENT '收藏目标ID（exhibition.id 或 gallery.id）',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_user_type` (`user_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';
