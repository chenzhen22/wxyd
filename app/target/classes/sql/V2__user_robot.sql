-- 用户管理重做 + 钉钉机器人 表结构（运营在 dev/prod MySQL 各执行一次）
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(256) NOT NULL COMMENT 'SM4 密文 hex',
  `display_name` VARCHAR(64) DEFAULT NULL,
  `role` TINYINT NOT NULL DEFAULT 1 COMMENT '0=超级管理员,1=普通',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=已通过,1=待审批,2=已拒绝',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ding_robot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `access_token` VARCHAR(256) NOT NULL,
  `secret` VARCHAR(512) NOT NULL COMMENT 'SM4 加密入库',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可选：备份旧 client 表后删除
-- RENAME TABLE `client` TO `client_bak_20260824`;
