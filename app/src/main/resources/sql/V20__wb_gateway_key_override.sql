-- V20: 模型网关 Key 级模型开关（管理员可对单条 Key 覆盖全局开关）
-- 全新安装由 DataInitializer 启动自检自动补列（本文件为手工执行备份）

ALTER TABLE `wb_gateway_key`
  ADD COLUMN `model_override` TINYINT NULL COMMENT 'NULL=跟随全局开关 1=强制开 0=强制关（静默映射免费模型）' AFTER `model`;
