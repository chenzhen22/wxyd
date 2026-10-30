ALTER TABLE `wb_gateway_config`
    ADD COLUMN `fallback_models` VARCHAR(1000) NOT NULL DEFAULT ''
    COMMENT '兜底模型（逗号分隔 id，免费模型都不可用时的最后手段，按顺序）' AFTER `free_priority`;
