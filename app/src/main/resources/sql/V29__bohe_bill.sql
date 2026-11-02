-- 薄荷账：账单 + 好友表（运营在 dev/prod MySQL 各执行一次）
CREATE TABLE IF NOT EXISTS `bohe_bill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT 'wxyd users.id',
  `cloud_id` VARCHAR(64) DEFAULT NULL COMMENT '小程序云数据库 _id，导入去重键',
  `amount` BIGINT NOT NULL COMMENT '金额，整数分',
  `category` VARCHAR(32) NOT NULL,
  `note` VARCHAR(255) DEFAULT NULL,
  `bill_date` DATE NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cloud_id` (`cloud_id`),
  KEY `idx_user_date` (`user_id`, `bill_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bohe_friend` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '发起方',
  `friend_id` BIGINT NOT NULL COMMENT '接收方',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待通过 1=已通过 2=已拒绝/删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pair` (`user_id`, `friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
