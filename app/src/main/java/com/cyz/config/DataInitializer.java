package com.cyz.config;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${wxyd.admin.username:admin}")
    private String adminUsername;

    @Value("${wxyd.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        ensureGithubLoginColumn();
        ensurePasswordNullable();
        ensureEmailColumn();
        ensureGiteeLoginColumn();
        int count = mysqlMapper.countUsers();
        if (count > 0) {
            log.info("[INIT] users 表已有 {} 个用户，跳过超管播种", count);
            return;
        }
        if (adminPassword == null || adminPassword.isEmpty()) {
            log.warn("[INIT] wxyd.admin.password 未配置，跳过超管播种（请在 env.properties 设置后重启）");
            return;
        }
        User u = new User();
        u.setUsername(adminUsername);
        u.setPassword(sm4KeyHolder.encrypt(adminPassword));
        u.setDisplayName("超级管理员");
        u.setRole(0);
        u.setStatus(0);
        mysqlMapper.insertUser(u);
        log.info("[INIT] 已初始化超级管理员: {}，请及时修改密码", adminUsername);
    }

    /**
     * 幂等确保 users 表存在 github_login 列（对应 sql/V7__github_login.sql）。
     * 部署环境未安装 mysql 客户端，故在启动时自检并自动补齐；已存在则跳过。
     * 若因权限等原因失败，仅记录提示，请手动执行 V7__github_login.sql。
     */
    private void ensureGithubLoginColumn() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='users' AND COLUMN_NAME='github_login'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] users.github_login 列已存在，跳过 V7 迁移");
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `users` " +
                    "ADD COLUMN `github_login` VARCHAR(64) NULL COMMENT 'GitHub 登录名，用于 OAuth 关联' AFTER `password`, " +
                    "ADD UNIQUE KEY `uk_github_login` (`github_login`)");
            log.info("[INIT] 已自动执行 V7 迁移：users 表增加 github_login 列");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V7 迁移失败（请手动执行 sql/V7__github_login.sql）：{}", e.getMessage());
        }
    }

    /**
     * 幂等确保 users.password 可空（对应 sql/V8__password_nullable.sql）：
     * GitHub 登录用户没有密码，password 为 NULL；密码登录路径已兼容 null（直接视为密码错误）。
     */
    private void ensurePasswordNullable() {
        try {
            String nullable = jdbcTemplate.queryForObject(
                    "SELECT IS_NULLABLE FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='users' AND COLUMN_NAME='password'",
                    String.class);
            if ("YES".equals(nullable)) {
                log.info("[INIT] users.password 已可空，跳过 V8 迁移");
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `users` " +
                    "MODIFY `password` VARCHAR(256) NULL COMMENT 'SM4 密文 hex（GitHub 登录用户为 NULL）'");
            log.info("[INIT] 已自动执行 V8 迁移：users.password 改为可空");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V8 迁移失败（请手动执行 sql/V8__password_nullable.sql）：{}", e.getMessage());
        }
    }

    /**
     * 幂等确保 users 表存在 email 列（对应 sql/V9__email_register.sql）：
     * 邮箱验证码注册流程写入；唯一索引防重复注册。
     */
    private void ensureEmailColumn() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='users' AND COLUMN_NAME='email'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] users.email 列已存在，跳过 V9 迁移");
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `users` " +
                    "ADD COLUMN `email` VARCHAR(128) NULL COMMENT '注册邮箱，邮箱注册流程写入' AFTER `github_login`, " +
                    "ADD UNIQUE KEY `uk_email` (`email`)");
            log.info("[INIT] 已自动执行 V9 迁移：users 表增加 email 列");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V9 迁移失败（请手动执行 sql/V9__email_register.sql）：{}", e.getMessage());
        }
    }

    /**
     * 幂等确保 users 表存在 gitee_login 列（对应 sql/V10__gitee_login.sql）：
     * Gitee OAuth 登录写入；唯一索引防重复关联。
     */
    private void ensureGiteeLoginColumn() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='users' AND COLUMN_NAME='gitee_login'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] users.gitee_login 列已存在，跳过 V10 迁移");
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `users` " +
                    "ADD COLUMN `gitee_login` VARCHAR(64) NULL COMMENT 'Gitee 登录名，用于 OAuth 关联' AFTER `email`, " +
                    "ADD UNIQUE KEY `uk_gitee_login` (`gitee_login`)");
            log.info("[INIT] 已自动执行 V10 迁移：users 表增加 gitee_login 列");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V10 迁移失败（请手动执行 sql/V10__gitee_login.sql）：{}", e.getMessage());
        }
    }
}
