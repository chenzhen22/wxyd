-- 群聊功能 表结构（运营在 dev/prod MySQL 各执行一次）
CREATE TABLE IF NOT EXISTS `group_chat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(20) NOT NULL,
  `owner_id` BIGINT NOT NULL COMMENT '创建者即管理员',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `group_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(8) NOT NULL DEFAULT 'member' COMMENT 'owner=管理员,member=成员',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `group_join_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `reason` VARCHAR(200) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待审,1=通过,2=拒绝',
  `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `handle_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_group_status` (`group_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 中转消息：每条群消息按“接收人”逐行落库；接收方拉取即删，全员拉走后服务端无残留；
-- expire_time = send_time + 7 天，用于长期离线成员导致的消息堆积兜底清理。
CREATE TABLE IF NOT EXISTS `group_msg_transit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL,
  `from_user_id` BIGINT NOT NULL,
  `to_user_id` BIGINT NOT NULL,
  `msg_type` VARCHAR(8) NOT NULL COMMENT 'text/emoji/image',
  `content` VARCHAR(1000) NOT NULL,
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_to_user` (`to_user_id`,`id`),
  KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
