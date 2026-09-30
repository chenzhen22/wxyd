-- GitHub 授权登录：users 表增加 github_login 列（可空、唯一），用于关联 GitHub 账号
-- 在 dev/prod MySQL 各执行一次
ALTER TABLE `users`
  ADD COLUMN `github_login` VARCHAR(64) NULL COMMENT 'GitHub 登录名，用于 OAuth 关联' AFTER `password`,
  ADD UNIQUE KEY `uk_github_login` (`github_login`);
