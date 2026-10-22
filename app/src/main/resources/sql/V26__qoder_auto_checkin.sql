-- V26: Qoder 每日自动领取签到积分（开关）
ALTER TABLE `wb_gateway_config`
  ADD COLUMN `qoder_auto_checkin` TINYINT NOT NULL DEFAULT 1 COMMENT '1=每天自动领取签到积分 0=关闭' AFTER `free_priority`;
