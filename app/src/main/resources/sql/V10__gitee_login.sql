-- Gitee 授权登录：users 表增加 gitee_login 列（可空、唯一），用于关联 Gitee 账号
-- 在 dev/prod MySQL 各执行一次
ALTER TABLE `users`
  ADD COLUMN `gitee_login` VARCHAR(64) NULL COMMENT 'Gitee 登录名，用于 OAuth 关联' AFTER `email`,
  ADD UNIQUE KEY `uk_gitee_login` (`gitee_login`);
