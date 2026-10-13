-- V14: 登录日志（对应 2026-10-13 登录日志功能）
-- 在 dev/prod MySQL 各执行一次；部署环境无 mysql 客户端时由 DataInitializer 启动自检自动建表
-- 保留 90 天，LoginLogService 每日 4 点定时清理

CREATE TABLE IF NOT EXISTS `login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL COMMENT '登录用户 id；失败尝试且用户不存在时为 NULL',
  `username` VARCHAR(64) NOT NULL COMMENT '尝试登录的用户名/邮箱，OAuth 为登录名',
  `event` VARCHAR(20) NOT NULL COMMENT 'login_success | login_fail | logout',
  `channel` VARCHAR(20) NOT NULL COMMENT 'password | github | gitee | email',
  `ip` VARCHAR(64) NULL COMMENT '客户端 IP',
  `detail` VARCHAR(255) NULL COMMENT '失败原因等补充信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志（保留 90 天）';
