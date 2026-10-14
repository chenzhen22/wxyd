-- 金融日报订阅改多订阅：一人可配多个钉钉机器人和多个邮箱（每行一个通道）
-- 原 V15 的 uk_user 唯一键限制一人一条，改为普通索引 idx_user
ALTER TABLE `day_info_subscribe`
  DROP KEY `uk_user`,
  ADD KEY `idx_user` (`user_id`);
