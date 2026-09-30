-- GitHub 授权登录：GitHub 用户没有密码，users.password 改为可空（V7 建的 github_login 列关联 GitHub 账号）
ALTER TABLE `users`
  MODIFY `password` VARCHAR(256) NULL COMMENT 'SM4 密文 hex（GitHub 登录用户为 NULL）';
