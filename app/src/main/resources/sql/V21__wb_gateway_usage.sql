-- V21: 模型网关令牌消耗统计（每次对话请求落一条）
-- 全新安装由 DataInitializer 启动自检自动建表（本文件为手工执行备份）

CREATE TABLE IF NOT EXISTS `wb_gateway_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '调用者',
  `key_id` BIGINT NOT NULL COMMENT '使用的网关 Key',
  `protocol` VARCHAR(16) NOT NULL DEFAULT 'anthropic' COMMENT 'anthropic | openai',
  `requested_model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端请求的模型名',
  `used_model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '实际使用的上游模型（映射后）',
  `stream` TINYINT NOT NULL DEFAULT 0 COMMENT '1=流式 0=非流式',
  `input_tokens` BIGINT NOT NULL DEFAULT 0,
  `output_tokens` BIGINT NOT NULL DEFAULT 0,
  `duration_ms` BIGINT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ok' COMMENT 'ok | error',
  `error_kind` VARCHAR(32) NULL COMMENT '失败类别（invalid_request/auth_error/...）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_used_model` (`used_model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型网关令牌消耗统计';
