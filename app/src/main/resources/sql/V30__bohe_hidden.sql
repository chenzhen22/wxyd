-- 薄荷账：好友可见开关（需求 4：我的页可设置不让好友看薄荷账）
-- 注意与已有 bohe_friend 建表脚本（V29）同为"运营执行一次"的迁移。
-- users 表加列：0=好友可见（默认），1=隐藏
ALTER TABLE `users` ADD COLUMN `bohe_hidden` TINYINT NOT NULL DEFAULT 0 COMMENT '薄荷账对好友隐藏：0=可见 1=隐藏';
