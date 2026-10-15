-- 金融日报订阅推送时间窗口：day_info 增加 notified 标记
-- 首次入库 notified=0（待推送），订阅提醒定时任务在时间窗口（默认 8-22 点）内发送后置 1；
-- 同日覆盖更新不重置（不重复提醒）。存量历史行默认 1，不回灌提醒。
ALTER TABLE `day_info`
  ADD COLUMN `notified` TINYINT NOT NULL DEFAULT 1 COMMENT '订阅提醒发送标记：0=待推送 1=已发送/无需发送' AFTER `brief`;
