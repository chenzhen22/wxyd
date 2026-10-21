-- V23: 模型网关 Key 仅当天有效（active_date 记录启用日期，次日自动停用）
-- 全新安装由 DataInitializer 启动自检自动建表/补列（本文件为手工执行备份）

ALTER TABLE `wb_gateway_key`
  ADD COLUMN `active_date` DATE NULL COMMENT '生效日期：仅当天有效，次日自动停用需用户重新启用' AFTER `enabled`;

-- 存量 Key 一次性停用（升级后需用户在「模型网关」页面重新点启用）
UPDATE `wb_gateway_key` SET `enabled` = 0 WHERE `active_date` IS NULL;
