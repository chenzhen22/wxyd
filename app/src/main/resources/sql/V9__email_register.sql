-- V9: 邮箱注册 — users 表增加 email 列（唯一），用于邮箱验证码注册流程
ALTER TABLE `users`
    ADD COLUMN `email` VARCHAR(128) NULL COMMENT '注册邮箱，邮箱注册流程写入' AFTER `github_login`,
    ADD UNIQUE KEY `uk_email` (`email`);
