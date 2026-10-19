package com.cyz.config;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.mapper.mysqlMapper.NoteIndexMapper;
import com.cyz.note.service.NoteDataService;
import com.cyz.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private NoteIndexMapper noteIndexMapper;

    @Autowired
    private NoteDataService noteDataService;

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
        ensureNoteIndexTable();
        ensureNoteIndexPopulated();
        ensureRadarPatrolTables();
        ensureLoginLogTable();
        ensureDayInfoTables();
        ensureWbGatewayTables();
        ensureMenuItems();
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

    /**
     * 幂等确保笔记检索索引表存在（对应 sql/V11__note_index.sql）：
     * 个人知识库 v2 的全文检索索引，md 文件仍为真相源，本表仅承载检索。
     * content 列只入库「属主自己笔记」的正文（产品决策 Q1）；他人 public 笔记仅存元数据，
     * content_indexed=0 表示正文未入库，查询层据此只做标题匹配。
     * 部署环境无 mysql 客户端，故启动时自检建表；已存在则跳过。
     * 失败仅记录提示，不阻断启动（检索可降级为目录扫描兜底）。
     */
    private void ensureNoteIndexTable() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='note_index'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] note_index 表已存在，跳过 V11 迁移");
                return;
            }
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `note_index` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "`owner_id` BIGINT NOT NULL COMMENT '笔记属主用户 id'," +
                    "`note_name` VARCHAR(64) NOT NULL COMMENT '净化后的标题，与文件名一致'," +
                    "`title` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'frontmatter.name 原始标题'," +
                    "`category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '分类，受控词表或用户自建'," +
                    "`tags` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '标签，逗号分隔'," +
                    "`source_key` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '来源条目 key，空=非摘录'," +
                    "`is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '1=公开 0=私有'," +
                    "`content_indexed` TINYINT NOT NULL DEFAULT 0 COMMENT '1=content 已入库 0=仅元数据（他人public不入库）'," +
                    "`content` MEDIUMTEXT COMMENT '正文全文，仅 content_indexed=1 时有值'," +
                    "`preview` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '列表预览，150 字'," +
                    "`owner_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '属主展示名，冗余避免列表 N+1'," +
                    "`update_time` DATETIME NOT NULL COMMENT '文件最后修改时间'," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_owner_note` (`owner_id`,`note_name`)," +
                    "KEY `idx_public_time` (`is_public`,`update_time`)," +
                    "KEY `idx_source` (`source_key`)," +
                    "KEY `idx_owner_time` (`owner_id`,`update_time`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记检索索引，md 文件为真相源'");
            log.info("[INIT] 已自动执行 V11 迁移：创建 note_index 笔记检索索引表");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V11 迁移失败（请手动执行 sql/V11__note_index.sql）：{}", e.getMessage());
        }
    }

    /**
     * 索引为空但磁盘有笔记时自动重建（对应 V11 note_index 的升级体验修复）。
     * <p>
     * 背景：v2 笔记列表/检索改走 note_index 索引，升级部署后若不手动执行
     * POST /note/rebuildIndex，老用户的笔记列表会显示为空 —— 看起来像"笔记丢了"，
     * 实际 md 文件完好。故启动时自检：索引空但笔记目录存在文件 → 自动重建一次。
     * 索引非空则跳过（避免每次启动都全量扫描）。
     */
    private void ensureNoteIndexPopulated() {
        try {
            Integer cnt = noteIndexMapper.countAll();
            if (cnt != null && cnt > 0) {
                log.info("[INIT] note_index 已有 {} 行，跳过自动重建", cnt);
                return;
            }
            int total = noteDataService.rebuildIndex();
            if (total > 0) {
                log.info("[INIT] 索引为空，已自动重建：共 {} 篇笔记入索引", total);
            } else {
                log.info("[INIT] 索引为空且笔记目录无文件，跳过重建（全新安装）");
            }
        } catch (Exception e) {
            log.error("[INIT] 笔记索引自动重建失败（可手动执行 POST /note/rebuildIndex）：{}", e.getMessage());
        }
    }

    /**
     * 幂等确保羊毛雷达/巡检日报三表存在（对应 sql/V12__radar_patrol.sql）。
     */
    private void ensureRadarPatrolTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `radar_subscribe` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "`user_id` BIGINT NOT NULL COMMENT '订阅属主'," +
                    "`keyword` VARCHAR(64) NOT NULL COMMENT '命中关键词（标题或摘要 LIKE）'," +
                    "`channel` VARCHAR(10) NOT NULL COMMENT 'dingtalk | email'," +
                    "`robot_id` BIGINT NULL COMMENT 'channel=dingtalk 时必填'," +
                    "`email` VARCHAR(128) NULL COMMENT 'channel=email 时必填'," +
                    "`last_post_id` BIGINT NOT NULL DEFAULT 0 COMMENT '已推送到的 scan_post.id'," +
                    "`enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=暂停'," +
                    "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_user_keyword_channel` (`user_id`,`keyword`,`channel`)," +
                    "KEY `idx_user` (`user_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='羊毛雷达订阅'");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `patrol_asset` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "`user_id` BIGINT NOT NULL," +
                    "`name` VARCHAR(64) NOT NULL COMMENT '资产展示名'," +
                    "`asset_type` VARCHAR(10) NOT NULL COMMENT 'url | domain | server'," +
                    "`target` VARCHAR(255) NOT NULL COMMENT 'url:完整URL / domain:域名 / server:host'," +
                    "`port` INT NULL COMMENT 'server 的 SSH 端口，默认 22'," +
                    "`ssh_user` VARCHAR(32) NULL," +
                    "`ssh_password` VARCHAR(256) NULL COMMENT 'SSH 密码，SM4 密文'," +
                    "`enabled` TINYINT NOT NULL DEFAULT 1," +
                    "`last_status` VARCHAR(10) NULL COMMENT 'ok | warn | fail | unknown'," +
                    "`last_detail` VARCHAR(1000) NULL," +
                    "`last_check_time` DATETIME NULL," +
                    "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`id`),KEY `idx_user` (`user_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检资产'");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `patrol_setting` (" +
                    "`user_id` BIGINT NOT NULL," +
                    "`push_hour` TINYINT NOT NULL DEFAULT 8 COMMENT '日报推送小时（0-23）'," +
                    "`channel` VARCHAR(10) NOT NULL COMMENT 'dingtalk | email'," +
                    "`robot_id` BIGINT NULL," +
                    "`email` VARCHAR(128) NULL," +
                    "`enabled` TINYINT NOT NULL DEFAULT 1," +
                    "PRIMARY KEY (`user_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检日报推送设置'");
            log.info("[INIT] 已自动执行 V12 迁移：radar_subscribe / patrol_asset / patrol_setting");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V12 迁移失败（请手动执行 sql/V12__radar_patrol.sql）：{}", e.getMessage());
        }
    }

    /** menu_item seed：{menuKey, platform, name, icon, hashUrl, sortNo, adminOnly, fixed}，desktop 在前 */
    private static final String[][] MENU_SEED = {
            {"userManage", "desktop", "用户管理", "👤", "#/userManage", "10", "0", "0"},
            {"messageBoard", "desktop", "留言板", "💬", "#/messageBoard", "20", "0", "0"},
            {"robotManage", "desktop", "钉钉机器人", "🤖", "#/robotManage", "30", "0", "0"},
            {"groupChat", "desktop", "群聊", "👥", "#/groupChat", "40", "0", "0"},
            {"javaApi", "desktop", "Java8-API", "☕", "#/javaApi", "50", "0", "0"},
            {"shellScript", "desktop", "Shell脚本", "🐚", "#/shellScript", "60", "0", "0"},
            {"note", "desktop", "记忆笔记", "📝", "#/note", "70", "0", "0"},
            {"shareFile", "desktop", "文件共享", "📁", "#/shareFile", "80", "0", "0"},
            {"pdfTool", "desktop", "证据材料整理", "📑", "#/pdfTool", "90", "0", "0"},
            {"sqlTool", "desktop", "SQL 查询", "🗄️", "#/sqlTool", "100", "1", "0"},
            {"dubboCall", "desktop", "Dubbo调用", "🔌", "#/dubboCall", "110", "0", "0"},
            {"archiveTool", "desktop", "归档下载", "📦", "#/archiveTool", "120", "0", "0"},
            {"welfare", "desktop", "福利专区", "🎁", "#/welfare", "130", "0", "0"},
            {"dayinfo", "desktop", "金融日报", "📈", "#/dayinfo", "135", "0", "0"},
            {"gateway", "desktop", "模型网关", "🔑", "#/gateway", "137", "0", "0"},
            {"patrol", "desktop", "巡检日报", "🛡️", "#/patrol", "140", "1", "0"},
            {"loginLog", "desktop", "登录日志", "📜", "#/loginLog", "145", "0", "0"},
            {"menuManage", "desktop", "菜单管理", "🧩", "#/menuManage", "150", "1", "1"},
            {"message", "h5", "留言板", "💬", "", "10", "0", "1"},
            {"group", "h5", "群聊", "👥", "", "20", "0", "1"},
            {"user", "h5", "用户管理", "👤", "#/user", "110", "1", "0"},
            {"welfare", "h5", "福利专区", "🎁", "#/welfare", "120", "0", "0"},
            {"dayinfo", "h5", "金融日报", "📈", "#/dayinfo", "125", "0", "0"},
            {"gateway", "h5", "模型网关", "🔑", "#/gateway", "127", "0", "0"},
            {"patrol", "h5", "巡检日报", "🛡️", "#/patrol", "130", "1", "0"},
            {"loginlog", "h5", "登录日志", "📜", "#/loginlog", "135", "0", "0"},
            {"robot", "h5", "钉钉机器人", "🤖", "#/robot", "140", "0", "0"},
            {"notes", "h5", "记忆笔记", "📝", "#/notes", "150", "0", "0"},
            {"share", "h5", "文件共享", "📁", "#/share", "160", "0", "0"},
            {"shell", "h5", "Shell脚本", "🐚", "#/shell", "170", "0", "0"},
            {"javaapi", "h5", "Java API", "☕", "#/javaapi", "180", "0", "0"},
            {"sql", "h5", "SQL查询", "🗄️", "#/sql", "190", "1", "0"},
            {"dubbo", "h5", "Dubbo调用", "🔌", "#/dubbo", "200", "0", "0"},
            {"archive", "h5", "归档下载", "📦", "#/archive", "210", "0", "0"},
            {"pdfTool", "h5", "证据材料整理", "📑", "#/pdfTool", "220", "0", "0"},
            {"menuManage", "h5", "菜单管理", "🧩", "#/menuManage", "230", "1", "1"},
    };

    /** 存量 menu_config 迁移映射：旧 desktop key → H5 key（旧 menu_config 用 desktop key 两端共用） */
    private static final String[][] MENU_KEY_MIGRATION = {
            {"userManage", "user"},
            {"messageBoard", "message"},
            {"robotManage", "robot"},
            {"groupChat", "group"},
            {"javaApi", "javaapi"},
            {"shellScript", "shell"},
            {"note", "notes"},
            {"shareFile", "share"},
            {"sqlTool", "sql"},
            {"dubboCall", "dubbo"},
            {"archiveTool", "archive"},
            {"welfare", "welfare"},
            {"patrol", "patrol"},
            {"pdfTool", "pdfTool"},
            {"menuManage", "menuManage"},
    };

    /**
     * V13 未发布前的开发库兜底：幂等给 menu_item 补 deleted 列（软删除，防止删除后重启被 seed/迁移复活）。
     */
    private void ensureMenuItemDeletedColumn() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='menu_item' AND COLUMN_NAME='deleted'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] menu_item.deleted 列已存在，跳过补列");
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `menu_item` " +
                    "ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '1=已软删除，列表不显示且不被 seed/迁移复活' AFTER `fixed`");
            log.info("[INIT] 已自动升级 menu_item：增加 deleted 列");
        } catch (Exception e) {
            log.error("[INIT] menu_item 补 deleted 列失败（请手动执行 sql/V13__menu_item.sql）：{}", e.getMessage());
        }
    }

    /** menu_config 表是否存在（全新安装可能尚未执行 V6__menu_config.sql） */
    private boolean menuConfigTableExists() {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='menu_config'",
                Integer.class);
        return cnt != null && cnt > 0;
    }

    /**
     * 幂等升级 menu_config 增加 platform 维度（旧表仅按 menu_key 唯一，h5/desktop 共用一行配置）。
     * 列与唯一键各自独立检查、独立执行：若上次升级在两条 ALTER 之间中断（已加列但唯一键仍是 uk_menu_key），
     * 整体跳过会导致 ON DUPLICATE KEY 仍按 menu_key 判重 —— h5 保存会覆盖 desktop 行。
     */
    private void ensureMenuConfigPlatform() {
        if (!menuConfigTableExists()) {
            log.info("[INIT] menu_config 表不存在（全新安装未执行 V6），跳过 platform 升级与权限迁移");
            return;
        }
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='menu_config' AND COLUMN_NAME='platform'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] menu_config.platform 列已存在，跳过补列");
            } else {
                jdbcTemplate.execute("ALTER TABLE `menu_config` " +
                        "ADD COLUMN `platform` VARCHAR(10) NOT NULL DEFAULT 'desktop' COMMENT 'h5 | desktop，旧数据视为桌面' AFTER `menu_key`");
                log.info("[INIT] 已自动升级 menu_config：增加 platform 列");
            }
        } catch (Exception e) {
            log.error("[INIT] menu_config 补 platform 列失败（请手动执行 sql/V13__menu_item.sql 升级段）：{}", e.getMessage());
        }
        try {
            Integer idx = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='menu_config' AND INDEX_NAME='uk_key_platform'",
                    Integer.class);
            if (idx != null && idx > 0) {
                log.info("[INIT] menu_config 唯一键 uk_key_platform 已存在，跳过索引升级");
                return;
            }
            Integer oldIdx = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='menu_config' AND INDEX_NAME='uk_menu_key'",
                    Integer.class);
            if (oldIdx != null && oldIdx > 0) {
                jdbcTemplate.execute("ALTER TABLE `menu_config` " +
                        "DROP INDEX `uk_menu_key`, ADD UNIQUE KEY `uk_key_platform` (`menu_key`, `platform`)");
            } else {
                jdbcTemplate.execute("ALTER TABLE `menu_config` " +
                        "ADD UNIQUE KEY `uk_key_platform` (`menu_key`, `platform`)");
            }
            log.info("[INIT] 已自动升级 menu_config：唯一键重建为 (menu_key, platform)");
        } catch (Exception e) {
            log.error("[INIT] menu_config 唯一键升级失败（请手动执行 sql/V13__menu_item.sql 升级段）：{}", e.getMessage());
        }
    }

    /**
     * 幂等确保金融日报两表存在（对应 sql/V15__day_info.sql）：
     * day_info 按 (type, info_date) 唯一，外部推送同日覆盖；day_info_subscribe 多订阅（V16 去一人一条限制）。
     */
    private void ensureDayInfoTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `day_info` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "`type` VARCHAR(32) NOT NULL DEFAULT 'finance' COMMENT '日报类型，finance=金融日报'," +
                    "`info_date` DATE NOT NULL COMMENT '日报所属日期（按 type+date 唯一）'," +
                    "`title` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '标题，默认「金融日报 YYYY-MM-DD」'," +
                    "`content` MEDIUMTEXT COMMENT 'md 原文'," +
                    "`brief` TEXT NULL COMMENT '简洁版内容，订阅推送用；空则后端自动截取正文'," +
                    "`notified` TINYINT NOT NULL DEFAULT 1 COMMENT '订阅提醒发送标记：0=待推送 1=已发送/无需发送'," +
                    "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_type_date` (`type`,`info_date`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金融日报（外部加密推送，每日一条）'");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `day_info_subscribe` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "`user_id` BIGINT NOT NULL COMMENT '订阅属主，一人多条'," +
                    "`channel` VARCHAR(10) NOT NULL COMMENT 'dingtalk | email'," +
                    "`robot_id` BIGINT NULL COMMENT 'channel=dingtalk 时必填'," +
                    "`email` VARCHAR(128) NULL COMMENT 'channel=email 时必填'," +
                    "`enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=暂停'," +
                    "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_user` (`user_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金融日报订阅提醒'");
            ensureDayInfoSubscribeMulti();
            ensureDayInfoBriefColumn();
            ensureDayInfoNotifiedColumn();
            log.info("[INIT] 已自动执行 V15-V18 迁移：day_info / day_info_subscribe（多订阅）/ brief / notified");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V15/V16 迁移失败（请手动执行 sql/V15__day_info.sql、V16__dayinfo_subscribe_multi.sql）：{}", e.getMessage());
        }
    }

    /**
     * V18 升级兜底：day_info 若无 notified 列（订阅提醒发送标记）则幂等补列。
     * 默认值 1：存量历史行不回灌提醒；新插入行由 service 显式写 0。
     */
    private void ensureDayInfoNotifiedColumn() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='day_info' AND COLUMN_NAME='notified'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] day_info.notified 列已存在，跳过 V18 补列");
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `day_info` " +
                    "ADD COLUMN `notified` TINYINT NOT NULL DEFAULT 1 " +
                    "COMMENT '订阅提醒发送标记：0=待推送 1=已发送/无需发送' AFTER `brief`");
            log.info("[INIT] 已自动执行 V18 迁移：day_info 增加 notified 列");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V18 迁移失败（请手动执行 sql/V18__day_info_notified.sql）：{}", e.getMessage());
        }
    }

    /**
     * V17 升级兜底：day_info 若无 brief 列（简洁版，订阅推送用）则幂等补列。
     */
    private void ensureDayInfoBriefColumn() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='day_info' AND COLUMN_NAME='brief'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] day_info.brief 列已存在，跳过 V17 补列");
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `day_info` " +
                    "ADD COLUMN `brief` TEXT NULL COMMENT '简洁版内容，订阅推送用；空则后端自动截取正文' AFTER `content`");
            log.info("[INIT] 已自动执行 V17 迁移：day_info 增加 brief 列");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V17 迁移失败（请手动执行 sql/V17__day_info_brief.sql）：{}", e.getMessage());
        }
    }

    /**
     * V16 升级兜底：旧库 day_info_subscribe 若仍带 uk_user 唯一键（一人一条），
     * 幂等改为普通索引 idx_user，支持一人多订阅（多个钉钉机器人 + 多个邮箱）。
     */
    private void ensureDayInfoSubscribeMulti() {
        try {
            Integer uk = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='day_info_subscribe' AND INDEX_NAME='uk_user'",
                    Integer.class);
            if (uk == null || uk == 0) {
                log.info("[INIT] day_info_subscribe 无 uk_user，跳过 V16 升级");
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `day_info_subscribe` " +
                    "DROP KEY `uk_user`, ADD KEY `idx_user` (`user_id`)");
            log.info("[INIT] 已自动执行 V16 迁移：day_info_subscribe 支持多订阅");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V16 迁移失败（请手动执行 sql/V16__dayinfo_subscribe_multi.sql）：{}", e.getMessage());
        }
    }

    /**
     * 幂等确保登录日志表存在（对应 sql/V14__login_log.sql）。
     * 部署环境无 mysql 客户端，故启动时自检建表；已存在则跳过。
     * 失败仅记录提示，不阻断启动（登录日志写入本身也是尽力而为）。
     */
    private void ensureLoginLogTable() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='login_log'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] login_log 表已存在，跳过 V14 建表");
                return;
            }
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `login_log` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "`user_id` BIGINT NULL COMMENT '登录用户 id；失败尝试且用户不存在时为 NULL'," +
                    "`username` VARCHAR(64) NOT NULL COMMENT '尝试登录的用户名/邮箱，OAuth 为登录名'," +
                    "`event` VARCHAR(20) NOT NULL COMMENT 'login_success | login_fail | logout'," +
                    "`channel` VARCHAR(20) NOT NULL COMMENT 'password | github | gitee | email'," +
                    "`ip` VARCHAR(64) NULL COMMENT '客户端 IP'," +
                    "`detail` VARCHAR(255) NULL COMMENT '失败原因等补充信息'," +
                    "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_user_id` (`user_id`)," +
                    "KEY `idx_create_time` (`create_time`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志（保留 90 天）'");
            log.info("[INIT] 已自动执行 V14 迁移：创建 login_log 登录日志表");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V14 迁移失败（请手动执行 sql/V14__login_log.sql）：{}", e.getMessage());
        }
    }

    /**
     * 幂等确保菜单注册表存在（对应 sql/V13__menu_item.sql）：
     * 1. 自检建表；2. 补 deleted 列兜底旧开发库；3. INSERT IGNORE 幂等补种（保留已有运营改动，软删行不受影响）；
     * 4. menu_config 升级 platform 维度；5. 存量 menu_config 权限迁移到 H5 key（仅当目标 h5 行存在且未软删）。
     * 失败仅记录提示，不阻断启动（菜单读取直接走 menu_item 表）。
     */
    /**
     * V19：AI 工具模型网关（wb_gateway_key 每用户 API Key + wb_gateway_config 模型开关）。
     * 幂等建表 + 配置行播种；失败仅记录提示，可手动执行 sql/V19__wb_gateway.sql。
     */
    private void ensureWbGatewayTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `wb_gateway_key` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "`user_id` BIGINT NOT NULL COMMENT 'Key 属主'," +
                    "`name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '备注名'," +
                    "`api_key` VARCHAR(255) NOT NULL COMMENT 'Key 明文的 SM4 密文（随机 IV，不可等值查询）'," +
                    "`key_hash` CHAR(64) NOT NULL COMMENT 'Key 明文的 SM3 摘要（等值查询用）'," +
                    "`model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户自选模型（空=按请求模型/默认），仅模型开关开启时生效'," +
                    "`model_override` TINYINT NULL COMMENT 'NULL=跟随全局开关 1=强制开 0=强制关（仅管理员可设）'," +
                    "`enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用'," +
                    "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "`last_used_time` DATETIME NULL COMMENT '最近一次网关调用时间'," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_key_hash` (`key_hash`)," +
                    "KEY `idx_user` (`user_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 工具网关 API Key（每用户最多 5 把）'");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `wb_gateway_config` (" +
                    "`id` BIGINT NOT NULL COMMENT '固定单行 id=1'," +
                    "`model_switch` TINYINT NOT NULL DEFAULT 1 COMMENT '1=开：用户自选模型生效 0=关：静默映射到 WorkBuddy 免费模型（对用户不可见）'," +
                    "`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型网关全局配置（仅管理员可改）'");
            jdbcTemplate.update("INSERT IGNORE INTO wb_gateway_config (id, model_switch) VALUES (1, 1)");
            ensureWbGatewayKeyOverrideColumn();
            log.info("[INIT] 已自动执行 V19 迁移：wb_gateway_key / wb_gateway_config（模型网关）");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V19 迁移失败（请手动执行 sql/V19__wb_gateway.sql）：{}", e.getMessage());
        }
    }
    /**
     * V20 升级兜底：wb_gateway_key 若无 model_override 列（Key 级模型开关）则幂等补列。
     */
    private void ensureWbGatewayKeyOverrideColumn() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='wb_gateway_key' AND COLUMN_NAME='model_override'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE `wb_gateway_key` ADD COLUMN `model_override` TINYINT NULL " +
                    "COMMENT 'NULL=跟随全局开关 1=强制开 0=强制关（静默映射免费模型，仅管理员可设）' AFTER `model`");
            log.info("[INIT] 已自动执行 V20 迁移：wb_gateway_key 补 model_override 列（Key 级模型开关）");
        } catch (Exception e) {
            log.error("[INIT] 自动执行 V20 迁移失败（请手动执行 sql/V20__wb_gateway_key_override.sql）：{}", e.getMessage());
        }
    }

    private void ensureMenuItems() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='menu_item'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[INIT] menu_item 表已存在，跳过 V13 建表");
            } else {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `menu_item` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                        "`menu_key` VARCHAR(50) NOT NULL COMMENT '前端模块名/路由名'," +
                        "`platform` VARCHAR(10) NOT NULL COMMENT 'h5 | desktop'," +
                        "`name` VARCHAR(50) NOT NULL COMMENT '显示名'," +
                        "`icon` VARCHAR(20) NOT NULL DEFAULT '' COMMENT 'emoji 图标'," +
                        "`hash_url` VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'hash 路由如 #/welfare；空=固定tab入口'," +
                        "`sort_no` INT NOT NULL DEFAULT 0," +
                        "`admin_only` TINYINT NOT NULL DEFAULT 0 COMMENT '1=仅管理员可见'," +
                        "`fixed` TINYINT NOT NULL DEFAULT 0 COMMENT '1=结构性入口，菜单管理页禁止删除'," +
                        "`deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '1=已软删除，列表不显示且不被 seed/迁移复活'," +
                        "`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (`id`)," +
                        "UNIQUE KEY `uk_key_platform` (`menu_key`,`platform`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单注册表'");
                log.info("[INIT] 已自动执行 V13 迁移：创建 menu_item 菜单注册表");
            }
            ensureMenuItemDeletedColumn();
            for (String[] row : MENU_SEED) {
                jdbcTemplate.update("INSERT IGNORE INTO menu_item " +
                                "(`menu_key`,`platform`,`name`,`icon`,`hash_url`,`sort_no`,`admin_only`,`fixed`) " +
                                "VALUES (?,?,?,?,?,?,?,?)",
                        row[0], row[1], row[2], row[3], row[4],
                        Integer.parseInt(row[5]), Integer.parseInt(row[6]), Integer.parseInt(row[7]));
            }
            ensureMenuConfigPlatform();
            if (!menuConfigTableExists()) {
                // 全新安装未执行 V6：无存量权限可迁移（跳过提示已在 ensureMenuConfigPlatform 打印）
                return;
            }
            Map<String, Map<String, Object>> configs = new HashMap<>();
            for (Map<String, Object> c : mysqlMapper.listMenuConfigs()) {
                configs.put(c.get("menuKey") + "|" + c.get("platform"), c);
            }
            int copied = 0;
            Set<String> itemKeys = new HashSet<>();
            for (Map<String, Object> item : mysqlMapper.listMenuItems()) {
                itemKeys.add(item.get("menuKey") + "|" + item.get("platform"));
            }
            for (String[] pair : MENU_KEY_MIGRATION) {
                // 旧 menu_config 不带 platform，升级后存量行被回填为 desktop
                Map<String, Object> src = configs.get(pair[0] + "|desktop");
                if (src == null || configs.containsKey(pair[1] + "|h5")) {
                    continue;
                }
                // 防止删除后重启被迁移逻辑复活：listMenuItems 已过滤 deleted=1，
                // 目标 h5 菜单项无行或已软删时跳过拷贝，否则会把旧 desktop 配置拷回并生成孤儿 menu_config 行
                if (!itemKeys.contains(pair[1] + "|h5")) {
                    continue;
                }
                mysqlMapper.upsertMenuConfig(pair[1], "h5", ((Number) src.get("visible")).intValue(),
                        (String) src.get("userIds"));
                copied++;
            }
            if (copied > 0) {
                log.info("[INIT] 已迁移 {} 条 menu_config 到 H5 key", copied);
            }
        } catch (Exception e) {
            log.error("[INIT] menu_item 建表/seed 失败（请手动执行 sql/V13__menu_item.sql）：{}", e.getMessage());
        }
    }
}
