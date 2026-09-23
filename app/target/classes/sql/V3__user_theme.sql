-- 用户主题偏好（运营在 dev/prod MySQL 各执行一次）
-- 记录每个用户选择的界面主题，'dark' = 暗色(默认)，'light' = 亮色
ALTER TABLE `users`
    ADD COLUMN `theme` VARCHAR(20) NOT NULL DEFAULT 'dark'
    COMMENT '界面主题：dark=暗色, light=亮色';
