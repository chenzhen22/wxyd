-- 金融日报：/common/dayinfo.do 推送入库（SM2 加密 + SM4 加签，后端验签解密）
-- day_info：每种类型每天一条（uk_type_date），同类型同日重复推送覆盖更新，只留最新
CREATE TABLE IF NOT EXISTS `day_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(32) NOT NULL DEFAULT 'finance' COMMENT '日报类型，finance=金融日报',
  `info_date` DATE NOT NULL COMMENT '日报所属日期（按 type+date 唯一）',
  `title` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '标题，默认「金融日报 YYYY-MM-DD」',
  `content` MEDIUMTEXT COMMENT 'md 原文',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_date` (`type`,`info_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金融日报（外部加密推送，每日一条）';

-- day_info_subscribe：日报频道订阅（独立于羊毛雷达关键词订阅），新日报入库时按通道提醒
CREATE TABLE IF NOT EXISTS `day_info_subscribe` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '订阅属主，一人一条',
  `channel` VARCHAR(10) NOT NULL COMMENT 'dingtalk | email',
  `robot_id` BIGINT NULL COMMENT 'channel=dingtalk 时必填',
  `email` VARCHAR(128) NULL COMMENT 'channel=email 时必填',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=暂停',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金融日报订阅提醒';
