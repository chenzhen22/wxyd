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
}
