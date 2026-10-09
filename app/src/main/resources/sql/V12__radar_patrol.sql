-- V12: 羊毛雷达 + 巡检日报（对应 docs/plans/2026-10-09-radar-patrol-pwa-design.md）
-- 在 dev/prod MySQL 各执行一次；部署环境无 mysql 客户端时由 DataInitializer 启动自检自动建表

-- O1 羊毛雷达订阅：每条 = 用户 × 关键词 × 通道；RadarPushJob 每小时增量扫描 scan_post 推送
CREATE TABLE IF NOT EXISTS `radar_subscribe` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '订阅属主',
  `keyword` VARCHAR(64) NOT NULL COMMENT '命中关键词（标题或摘要 LIKE）',
  `channel` VARCHAR(10) NOT NULL COMMENT 'dingtalk | email',
  `robot_id` BIGINT NULL COMMENT 'channel=dingtalk 时必填，指向用户自己的 ding_robot.id',
  `email` VARCHAR(128) NULL COMMENT 'channel=email 时必填',
  `last_post_id` BIGINT NOT NULL DEFAULT 0 COMMENT '已推送到的 scan_post.id（增量水位）',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=暂停',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_keyword_channel` (`user_id`,`keyword`,`channel`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='羊毛雷达订阅';

-- O2 巡检资产：url 存活 / domain WHOIS 到期 / server SSH 磁盘内存
CREATE TABLE IF NOT EXISTS `patrol_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(64) NOT NULL COMMENT '资产展示名',
  `asset_type` VARCHAR(10) NOT NULL COMMENT 'url | domain | server',
  `target` VARCHAR(255) NOT NULL COMMENT 'url:完整URL / domain:域名 / server:host',
  `port` INT NULL COMMENT 'server 的 SSH 端口，默认 22',
  `ssh_user` VARCHAR(32) NULL COMMENT 'server 的 SSH 用户',
  `ssh_password` VARCHAR(256) NULL COMMENT 'SSH 密码，SM4 密文（与 ding_robot.secret 同待遇）',
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `last_status` VARCHAR(10) NULL COMMENT 'ok | warn | fail | unknown',
  `last_detail` VARCHAR(1000) NULL COMMENT '最近一次巡检详情',
  `last_check_time` DATETIME NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检资产';

-- O2 日报推送设置：每用户一条；未配置的用户不推日报（页面仍可见 last_status）
CREATE TABLE IF NOT EXISTS `patrol_setting` (
  `user_id` BIGINT NOT NULL,
  `push_hour` TINYINT NOT NULL DEFAULT 8 COMMENT '日报推送小时（0-23）',
  `channel` VARCHAR(10) NOT NULL COMMENT 'dingtalk | email',
  `robot_id` BIGINT NULL,
  `email` VARCHAR(128) NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检日报推送设置';
