-- ============================================================
-- 小泽助手（wxyd）应用初始化脚本
-- 适用：MySQL 8.0+；全新安装执行一次：mysql -u root -p < wxyd.sql
--
-- 说明：
-- 1. 本脚本已合并 sql/V2~V18 全部增量迁移的最终表结构（含索引与表参数），
--    全新库只需执行本脚本；存量旧库无需执行，应用启动时 DataInitializer
--    会自动自检补列/建表/升级（V7~V18）。
-- 2. 超级管理员、菜单项（menu_item）等种子数据由应用首次启动自动播种，
--    本脚本不含业务数据（仅保留遗留网关的 action/pwmanagement 种子）。
-- 3. 结构来源：以生产库实际 schema 为准整理，乱码注释已修正；
--    day_info 等新表字段与代码中 DataInitializer 的建表语句保持一致。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `wxyd` DEFAULT CHARSET = utf8mb4;

USE `wxyd`;

-- ============================================================
-- 一、用户与钉钉机器人
-- ============================================================

CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(256) DEFAULT NULL COMMENT 'SM4 密文 hex（GitHub 登录用户为 NULL）',
  `github_login` varchar(64) DEFAULT NULL COMMENT 'GitHub 登录名，用于 OAuth 关联',
  `email` varchar(128) DEFAULT NULL COMMENT '注册邮箱，邮箱注册流程写入',
  `gitee_login` varchar(64) DEFAULT NULL COMMENT 'Gitee 登录名，用于 OAuth 关联',
  `display_name` varchar(64) DEFAULT NULL COMMENT '显示昵称',
  `role` tinyint NOT NULL DEFAULT '1' COMMENT '0=超级管理员,1=普通用户',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0=已通过,1=待审批,2=已拒绝,3=已暂停',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `theme` varchar(20) NOT NULL DEFAULT 'dark' COMMENT '界面主题：dark=暗色, light=亮色',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_github_login` (`github_login`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_gitee_login` (`gitee_login`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS `ding_robot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '机器人属主用户 id',
  `name` varchar(128) NOT NULL COMMENT '机器人名称',
  `access_token` varchar(256) NOT NULL COMMENT '钉钉机器人 access_token',
  `secret` varchar(512) NOT NULL COMMENT 'SM4 加密入库',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '钉钉机器人';

-- ============================================================
-- 二、留言板
-- ============================================================

CREATE TABLE IF NOT EXISTS `message` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ip` varchar(64) NOT NULL,
  `info` varchar(614) DEFAULT NULL COMMENT '留言内容',
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint DEFAULT NULL COMMENT '留言用户 id',
  PRIMARY KEY (`id`),
  KEY `message_index` (`ip`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '留言板';

-- ============================================================
-- 三、群聊
-- ============================================================

CREATE TABLE IF NOT EXISTS `group_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(20) NOT NULL COMMENT '群名',
  `owner_id` bigint NOT NULL COMMENT '创建者即管理员',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群聊';

CREATE TABLE IF NOT EXISTS `group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(8) NOT NULL DEFAULT 'member' COMMENT 'owner=管理员,member=成员',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群成员';

CREATE TABLE IF NOT EXISTS `group_join_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `reason` varchar(200) DEFAULT NULL COMMENT '申请理由',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=待审,1=通过,2=拒绝',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `handle_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_group_status` (`group_id`,`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '入群申请';

CREATE TABLE IF NOT EXISTS `group_msg` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `from_user_id` bigint NOT NULL,
  `read_total` int NOT NULL DEFAULT '0' COMMENT '发送时的接收人数',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_group_from` (`group_id`,`from_user_id`),
  KEY `idx_expire` (`expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群消息（阅后即焚水位）';

CREATE TABLE IF NOT EXISTS `group_msg_read` (
  `msg_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`msg_id`,`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群消息已读记录';

CREATE TABLE IF NOT EXISTS `group_msg_transit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `msg_id` bigint NOT NULL DEFAULT '0' COMMENT '关联 group_msg.id',
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  `msg_type` varchar(8) NOT NULL COMMENT 'text/emoji/image',
  `content` varchar(1000) NOT NULL,
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_to_user` (`to_user_id`,`id`),
  KEY `idx_expire` (`expire_time`),
  KEY `idx_msg_id` (`msg_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群消息投递（每人一条，过期清理）';

-- ============================================================
-- 四、菜单
-- ============================================================

CREATE TABLE IF NOT EXISTS `menu_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_key` varchar(50) NOT NULL COMMENT '前端模块名/路由名',
  `platform` varchar(10) NOT NULL DEFAULT 'desktop' COMMENT 'h5 | desktop，旧数据视为桌面',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '1=可见 0=隐藏',
  `user_ids` varchar(2000) NOT NULL DEFAULT '' COMMENT '逗号分隔用户id',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_platform` (`menu_key`,`platform`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单可见性配置';

CREATE TABLE IF NOT EXISTS `menu_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_key` varchar(50) NOT NULL COMMENT '前端模块名/路由名',
  `platform` varchar(10) NOT NULL COMMENT 'h5 | desktop',
  `name` varchar(50) NOT NULL COMMENT '显示名',
  `icon` varchar(20) NOT NULL DEFAULT '' COMMENT 'emoji 图标',
  `hash_url` varchar(100) NOT NULL DEFAULT '' COMMENT 'hash 路由如 #/welfare；空=固定tab入口',
  `sort_no` int NOT NULL DEFAULT '0',
  `admin_only` tinyint NOT NULL DEFAULT '0' COMMENT '1=仅管理员可见',
  `fixed` tinyint NOT NULL DEFAULT '0' COMMENT '1=结构性入口，菜单管理页禁止删除',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '1=已软删除，列表不显示且不被 seed/迁移复活',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_platform` (`menu_key`,`platform`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单注册表';

-- ============================================================
-- 五、记忆笔记检索索引（md 文件为真相源）
-- ============================================================

CREATE TABLE IF NOT EXISTS `note_index` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_id` bigint NOT NULL COMMENT '笔记属主用户 id',
  `note_name` varchar(64) NOT NULL COMMENT '净化后的标题，与文件名一致',
  `title` varchar(255) NOT NULL DEFAULT '' COMMENT 'frontmatter.name 原始标题',
  `category` varchar(64) NOT NULL DEFAULT '' COMMENT '分类，受控词表或用户自建',
  `tags` varchar(500) NOT NULL DEFAULT '' COMMENT '标签，逗号分隔',
  `source_key` varchar(128) NOT NULL DEFAULT '' COMMENT '来源条目 key，空=非摘录',
  `is_public` tinyint NOT NULL DEFAULT '1' COMMENT '1=公开 0=私有',
  `content_indexed` tinyint NOT NULL DEFAULT '0' COMMENT '1=content 已入库 0=仅元数据（他人public不入库）',
  `content` mediumtext COMMENT '正文全文，仅 content_indexed=1 时有值',
  `preview` varchar(512) NOT NULL DEFAULT '' COMMENT '列表预览，150 字',
  `owner_name` varchar(64) NOT NULL DEFAULT '' COMMENT '属主展示名，冗余避免列表 N+1',
  `update_time` datetime NOT NULL COMMENT '文件最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_note` (`owner_id`,`note_name`),
  KEY `idx_public_time` (`is_public`,`update_time`),
  KEY `idx_source` (`source_key`),
  KEY `idx_owner_time` (`owner_id`,`update_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '笔记检索索引，md 文件为真相源';

-- ============================================================
-- 六、羊毛雷达 / 巡检日报
-- ============================================================

CREATE TABLE IF NOT EXISTS `radar_subscribe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '订阅属主',
  `keyword` varchar(64) NOT NULL COMMENT '命中关键词（标题或摘要 LIKE）',
  `channel` varchar(10) NOT NULL COMMENT 'dingtalk | email',
  `robot_id` bigint DEFAULT NULL COMMENT 'channel=dingtalk 时必填',
  `email` varchar(128) DEFAULT NULL COMMENT 'channel=email 时必填',
  `last_post_id` bigint NOT NULL DEFAULT '0' COMMENT '已推送到的 scan_post.id',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1=启用 0=暂停',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_keyword_channel` (`user_id`,`keyword`,`channel`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '羊毛雷达订阅';

-- patrol_asset 已废弃（巡检改为零配置系统自检），由 DataInitializer 启动时 DROP
CREATE TABLE IF NOT EXISTS `patrol_setting` (
  `user_id` bigint NOT NULL,
  `push_hour` tinyint NOT NULL DEFAULT '8' COMMENT '日报推送小时（0-23）',
  `channel` varchar(10) NOT NULL COMMENT 'dingtalk | email',
  `robot_id` bigint DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '巡检日报推送设置';

-- ============================================================
-- 七、登录日志
-- ============================================================

CREATE TABLE IF NOT EXISTS `login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '登录用户 id；失败尝试且用户不存在时为 NULL',
  `username` varchar(64) NOT NULL COMMENT '尝试登录的用户名/邮箱，OAuth 为登录名',
  `event` varchar(20) NOT NULL COMMENT 'login_success | login_fail | logout',
  `channel` varchar(20) NOT NULL COMMENT 'password | github | gitee | email',
  `ip` varchar(64) DEFAULT NULL COMMENT '客户端 IP',
  `detail` varchar(255) DEFAULT NULL COMMENT '失败原因等补充信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志（保留 90 天）';

-- ============================================================
-- 八、福利专区 / 羊毛雷达数据源（dataScan 写入）
-- ============================================================

CREATE TABLE IF NOT EXISTS `scan_source` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL COMMENT '数据源唯一编码，如 linuxdo_welfare',
  `name` varchar(128) NOT NULL COMMENT '显示名称',
  `source_type` varchar(32) NOT NULL DEFAULT 'discourse' COMMENT '抓取器类型(fetcher注册键)',
  `site_url` varchar(255) NOT NULL COMMENT '站点根地址',
  `config` json NOT NULL COMMENT '抓取配置(分类slug/id、过滤条件等)',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抓取数据源配置表';

CREATE TABLE IF NOT EXISTS `scan_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` int NOT NULL COMMENT '关联scan_source.id',
  `external_id` varchar(64) NOT NULL COMMENT '源站帖子ID',
  `url` varchar(512) NOT NULL COMMENT '帖子链接',
  `title` varchar(512) NOT NULL COMMENT '标题',
  `author` varchar(128) DEFAULT NULL COMMENT '作者',
  `tags` json DEFAULT NULL COMMENT '标签数组',
  `published_at` datetime DEFAULT NULL COMMENT '源站发布时间(东八区)',
  `content_md` mediumtext COMMENT '正文Markdown',
  `content_html` mediumtext COMMENT '正文原始HTML',
  `image_count` int NOT NULL DEFAULT '0' COMMENT '正文图片数',
  `images` json DEFAULT NULL COMMENT '图片列表[{url,alt}]',
  `extra` json DEFAULT NULL COMMENT '扩展信息(点赞/浏览/回复数等，新字段进这里)',
  `first_crawled_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_crawled_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_source_external` (`source_id`,`external_id`),
  KEY `idx_post_published` (`published_at`),
  KEY `idx_post_source_pub` (`source_id`,`published_at`),
  CONSTRAINT `fk_scan_post_source` FOREIGN KEY (`source_id`) REFERENCES `scan_source` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抓取的帖子正文表';

CREATE TABLE IF NOT EXISTS `scan_crawl_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` int DEFAULT NULL,
  `run_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(16) NOT NULL COMMENT 'success/partial/failed',
  `topics_fetched` int NOT NULL DEFAULT '0' COMMENT '本轮获取帖子数',
  `topics_new` int NOT NULL DEFAULT '0',
  `topics_updated` int NOT NULL DEFAULT '0',
  `error_msg` text,
  PRIMARY KEY (`id`),
  KEY `idx_log_run` (`run_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抓取运行日志';

-- ============================================================
-- 九、金融日报（外部加密推送）
-- ============================================================

CREATE TABLE IF NOT EXISTS `day_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(32) NOT NULL DEFAULT 'finance' COMMENT '日报类型，finance=金融日报',
  `info_date` date NOT NULL COMMENT '日报所属日期（按 type+date 唯一）',
  `title` varchar(255) NOT NULL DEFAULT '' COMMENT '标题，默认「金融日报 YYYY-MM-DD」',
  `content` mediumtext COMMENT 'md 原文',
  `brief` text COMMENT '简洁版内容，订阅推送用；空则后端自动截取正文',
  `notified` tinyint NOT NULL DEFAULT '1' COMMENT '订阅提醒发送标记：0=待推送 1=已发送/无需发送',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_date` (`type`,`info_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '金融日报（外部加密推送，每日一条）';

CREATE TABLE IF NOT EXISTS `day_info_subscribe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '订阅属主，一人多条（V16：多个钉钉机器人/多个邮箱）',
  `channel` varchar(10) NOT NULL COMMENT 'dingtalk | email',
  `robot_id` bigint DEFAULT NULL COMMENT 'channel=dingtalk 时必填',
  `email` varchar(128) DEFAULT NULL COMMENT 'channel=email 时必填',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1=启用 0=暂停',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '金融日报订阅提醒';

-- ============================================================
-- 十、遗留网关（tbphx.do）表，可按需保留
-- ============================================================

CREATE TABLE IF NOT EXISTS `action` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `actions` varchar(40) NOT NULL,
  PRIMARY KEY (`id`, `actions`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO `action` (id, actions) VALUES (1, 'queryMsgCode');
INSERT IGNORE INTO `action` (id, actions) VALUES (2, 'uploadFileToBank');
INSERT IGNORE INTO `action` (id, actions) VALUES (4, 'message');
INSERT IGNORE INTO `action` (id, actions) VALUES (5, 'queryMessage');
INSERT IGNORE INTO `action` (id, actions) VALUES (6, 'queryUdInfo');
INSERT IGNORE INTO `action` (id, actions) VALUES (7, 'start');
INSERT IGNORE INTO `action` (id, actions) VALUES (8, 'orderCreate');
INSERT IGNORE INTO `action` (id, actions) VALUES (9, 'error');
INSERT IGNORE INTO `action` (id, actions) VALUES (10, 'queryMsgCode');
INSERT IGNORE INTO `action` (id, actions) VALUES (11, 'uploadFileToBank');
INSERT IGNORE INTO `action` (id, actions) VALUES (13, 'message');
INSERT IGNORE INTO `action` (id, actions) VALUES (14, 'queryMessage');
INSERT IGNORE INTO `action` (id, actions) VALUES (15, 'queryUdInfo');
INSERT IGNORE INTO `action` (id, actions) VALUES (16, 'addWhite');
INSERT IGNORE INTO `action` (id, actions) VALUES (17, 'queryWhiteInfo');
INSERT IGNORE INTO `action` (id, actions) VALUES (18, 'updateWhite');
INSERT IGNORE INTO `action` (id, actions) VALUES (19, 'deleteWhite');
INSERT IGNORE INTO `action` (id, actions) VALUES (20, 'queryUdOper');
INSERT IGNORE INTO `action` (id, actions) VALUES (21, 'queryMsgCode');
INSERT IGNORE INTO `action` (id, actions) VALUES (22, 'uploadFileToBank');
INSERT IGNORE INTO `action` (id, actions) VALUES (23, 'mmLogin');
INSERT IGNORE INTO `action` (id, actions) VALUES (24, 'addMessage');
INSERT IGNORE INTO `action` (id, actions) VALUES (25, 'queryMessage');
INSERT IGNORE INTO `action` (id, actions) VALUES (26, 'queryUdInfo');
INSERT IGNORE INTO `action` (id, actions) VALUES (27, 'start');
INSERT IGNORE INTO `action` (id, actions) VALUES (28, 'orderCreate');
INSERT IGNORE INTO `action` (id, actions) VALUES (29, 'error');
INSERT IGNORE INTO `action` (id, actions) VALUES (30, 'addWhite');
INSERT IGNORE INTO `action` (id, actions) VALUES (31, 'queryWhiteInfo');
INSERT IGNORE INTO `action` (id, actions) VALUES (32, 'updateWhite');
INSERT IGNORE INTO `action` (id, actions) VALUES (33, 'deleteWhite');
INSERT IGNORE INTO `action` (id, actions) VALUES (34, 'queryUdOper');
INSERT IGNORE INTO `action` (id, actions) VALUES (35, 'docQry');
INSERT IGNORE INTO `action` (id, actions) VALUES (36, 'delMessage');
INSERT IGNORE INTO `action` (id, actions) VALUES (37, 'uploadDocumentFile');
INSERT IGNORE INTO `action` (id, actions) VALUES (38, 'queryDocumentFileList');
INSERT IGNORE INTO `action` (id, actions) VALUES (39, 'deleteDocumentFile');

CREATE TABLE IF NOT EXISTS `pwmanagement` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `type` varchar(20) NOT NULL,
  `password` varchar(20) NOT NULL,
  `desc` varchar(20),
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO `pwmanagement` (`type`, `password`, `desc`)
VALUES ('restart', '~1q2w3e4r', '重启应用密码');

CREATE TABLE IF NOT EXISTS `requestlog` (
  `clinetIp` varchar(20) NOT NULL,
  `url` varchar(200) NOT NULL,
  `data` varchar(6000),
  `now` varchar(20)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `doc` (
  `transcode` varchar(60),
  `transname` varchar(80),
  `project` varchar(60),
  `blname` varchar(100),
  `type` varchar(10),
  PRIMARY KEY (`transcode`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `docFile` (
  `fileName` varchar(60),
  `type` varchar(10),
  `time` varchar(60),
  PRIMARY KEY (`fileName`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `documentFile` (
  `fileUUID` varchar(60),
  `clientIp` varchar(30),
  `userName` varchar(80),
  `fileName` varchar(200),
  `createTime` varchar(20),
  PRIMARY KEY (`fileUUID`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `note` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `ip` varchar(64) NOT NULL,
  `string` varchar(6400) NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `operinfo` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `ip` varchar(64) NOT NULL,
  `info` varchar(256),
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `operinfo_index` (`ip`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `client` (
  `ip` varchar(64) NOT NULL,
  `status` char(1) NOT NULL,
  `username` varchar(614) NOT NULL,
  PRIMARY KEY (`ip`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `socketMessage` (
  `date` varchar(20),
  `clientIp` varchar(20),
  `serverIp` varchar(20),
  `type` char(1) NOT NULL,
  `url` varchar(614),
  `message` varchar(6000),
  `recive` varchar(6000),
  `status` char(1),
  `handleIp` varchar(20),
  PRIMARY KEY (`date`),
  KEY `socketMessage_index1` (`type`),
  KEY `socketMessage_index2` (`status`),
  KEY `socketMessage_index3` (`clientIp`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
