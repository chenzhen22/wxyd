-- V19: AI 工具模型网关（WorkBuddy 上游，双协议 /anthropic/** /openai/**）
-- 全新安装由 DataInitializer 启动自检自动建表（本文件为手工执行备份）

CREATE TABLE IF NOT EXISTS `wb_gateway_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT 'Key 属主',
  `name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '备注名',
  `api_key` VARCHAR(255) NOT NULL COMMENT 'Key 明文的 SM4 密文（随机 IV，不可等值查询）',
  `key_hash` CHAR(64) NOT NULL COMMENT 'Key 明文的 SM3 摘要（等值查询用）',
  `model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户自选模型（空=按请求模型/默认），仅模型开关开启时生效',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  `active_date` DATE NULL COMMENT '生效日期：仅当天有效，次日自动停用需用户重新启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_used_time` DATETIME NULL COMMENT '最近一次网关调用时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_hash` (`key_hash`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 工具网关 API Key（每用户最多 5 把）';

CREATE TABLE IF NOT EXISTS `wb_gateway_config` (
  `id` BIGINT NOT NULL COMMENT '固定单行 id=1',
  `model_switch` TINYINT NOT NULL DEFAULT 1 COMMENT '1=开：用户自选模型生效 0=关：静默映射到 WorkBuddy 免费模型（对用户不可见）',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型网关全局配置（仅管理员可改）';

INSERT IGNORE INTO `wb_gateway_config` (`id`, `model_switch`) VALUES (1, 1);
