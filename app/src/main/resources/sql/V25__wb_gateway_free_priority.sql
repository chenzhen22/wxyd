-- V25: 免费模型手动优先级（模型开关关闭时的静默映射顺序）
-- 空 = 按目录自动顺序；手动配置的模型失效时自动回退
ALTER TABLE `wb_gateway_config`
  ADD COLUMN `free_priority` VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '免费模型手动优先级（逗号分隔 id，空=自动）' AFTER `model_switch`;
