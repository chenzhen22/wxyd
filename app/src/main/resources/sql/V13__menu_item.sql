-- V13: 菜单注册表 menu_item（对应菜单模块化实施计划）
-- 在 dev/prod MySQL 各执行一次；部署环境无 mysql 客户端时由 DataInitializer 启动自检自动建表

-- 菜单注册表（H5 与桌面按平台各一行，同一功能互不共享配置）
-- 权限仍由 menu_config 承载（visible × user_ids 四态），其 menu_key 指向 (menu_key, platform) 行。
CREATE TABLE IF NOT EXISTS `menu_item` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT,
  `menu_key`   VARCHAR(50) NOT NULL COMMENT '前端模块名/路由名',
  `platform`   VARCHAR(10) NOT NULL COMMENT 'h5 | desktop',
  `name`       VARCHAR(50) NOT NULL COMMENT '显示名',
  `icon`       VARCHAR(20) NOT NULL DEFAULT '' COMMENT 'emoji 图标',
  `hash_url`   VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'hash 路由如 #/welfare；空=固定tab入口',
  `sort_no`    INT NOT NULL DEFAULT 0,
  `admin_only` TINYINT NOT NULL DEFAULT 0 COMMENT '1=仅管理员可见',
  `fixed`      TINYINT NOT NULL DEFAULT 0 COMMENT '1=结构性入口，菜单管理页禁止删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_platform` (`menu_key`, `platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单注册表';

-- seed 数据由 DataInitializer 启动自检幂等补种（项目惯例），也可参照 DataInitializer 中 MENU_SEED 常量手工 INSERT。

-- 旧 menu_config 表升级（V6 建表时未含 platform，本文件未部署过的全新库可跳过）：
-- 增加 platform 维度，同一功能在 h5/desktop 各一行配置；
-- DEFAULT 'desktop' 会把存量行回填为桌面配置，之后唯一键改为 (menu_key, platform)。
ALTER TABLE `menu_config` ADD COLUMN `platform` VARCHAR(10) NOT NULL DEFAULT 'desktop' COMMENT 'h5 | desktop，旧数据视为桌面' AFTER `menu_key`;
ALTER TABLE `menu_config` DROP INDEX `uk_menu_key`, ADD UNIQUE KEY `uk_key_platform` (`menu_key`, `platform`);
