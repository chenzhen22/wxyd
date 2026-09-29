-- 群聊已读跟踪 表结构（运营在 dev/prod MySQL 各执行一次）
-- 消息登记表：发送时逐条登记，记录应达人数；用于已读计数/明细，7 天过期兜底
CREATE TABLE IF NOT EXISTS `group_msg` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL,
  `from_user_id` BIGINT NOT NULL,
  `read_total` INT NOT NULL DEFAULT 0 COMMENT '发送时的接收人数',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_group_from` (`group_id`,`from_user_id`),
  KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 已读记录：接收方拉取中转消息时写入（msg_id+user_id 唯一，防重）
CREATE TABLE IF NOT EXISTS `group_msg_read` (
  `msg_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`msg_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 中转消息关联消息登记 id
ALTER TABLE `group_msg_transit`
  ADD COLUMN `msg_id` BIGINT NOT NULL DEFAULT 0 COMMENT '关联 group_msg.id' AFTER `group_id`,
  ADD KEY `idx_msg_id` (`msg_id`);
