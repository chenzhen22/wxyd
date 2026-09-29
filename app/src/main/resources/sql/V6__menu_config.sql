-- 菜单管理 表结构（运营在 dev/prod MySQL 各执行一次）
-- 四态模型：visible=1 且 user_ids 空 → 所有人可见；
--          visible=0 且 user_ids 空 → 所有人不可见；
--          visible=1 且 user_ids 非空 → 仅名单内用户可见（白名单）；
--          visible=0 且 user_ids 非空 → 名单内用户不可见（黑名单），其他人可见。
-- 管理员(role=0)不受配置影响，始终可见全部菜单。未配置的菜单默认所有人可见。
CREATE TABLE IF NOT EXISTS `menu_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `menu_key` VARCHAR(50) NOT NULL,
  `visible` TINYINT NOT NULL DEFAULT 1,
  `user_ids` VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '逗号分隔用户id',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_key` (`menu_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
