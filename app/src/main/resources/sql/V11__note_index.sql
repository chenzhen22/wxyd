-- V11: 个人知识库 — 笔记检索索引表（对应 docs/plans/2026-10-07-knowledge-base-prd.md R1）
-- 在 dev/prod MySQL 各执行一次；部署环境无 mysql 客户端时由 DataInitializer 启动自检自动建表
--
-- 设计要点：
--   1. md 文件仍是唯一真相源（保留 v1 的可导出、可手工编辑优势），本表仅作检索加速。
--   2. 【全文检索只对属主生效】产品决策 Q1：任何人保存自己的笔记时正文都会入库
--      （否则本人无法全文检索自己的笔记，这是功能本身的要求）。
--      Q1 的保证来自**查询条件**，不是来自"库里没有这份数据"：
--        - 正文命中条件必须写作 `owner_id = #{currentUserId} AND content_indexed = 1`
--        - 漏掉 owner_id 限定会让他人搜到你的 public 笔记正文（已实测踩过，见 NoteIndexMapper.xml 注释）
--      → 表现：他人 public 笔记只能按标题命中，搜不到正文内容。这是已知且已接受的取舍。
--   3. 查询必须强制 `is_public = 1 OR owner_id = #{currentUserId}`，
--      private 笔记绝不出现在他人结果里；content 列再叠一层纵深防御。
--   4. uk_owner_note(owner_id, note_name) 保证一用户一标题，与 v1 的「同用户标题唯一」一致。
--   5. 索引与文件可能漂移（手工改文件/删文件），由 POST /note/rebuildIndex 幂等重建兜底。
CREATE TABLE IF NOT EXISTS `note_index` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_id` BIGINT NOT NULL COMMENT '笔记属主用户 id',
  `note_name` VARCHAR(64) NOT NULL COMMENT '净化后的标题，与文件名一致',
  `title` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'frontmatter.name 原始标题',
  `category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '分类，受控词表或用户自建',
  `tags` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '标签，逗号分隔',
  `source_key` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '来源条目 key（javaapi/xxx、shell/xxx），空=非摘录',
  `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '1=公开 0=私有',
  `content_indexed` TINYINT NOT NULL DEFAULT 0 COMMENT '1=本篇正文已入库（仅 owner_id 本人可做正文命中检索）',
  `content` MEDIUMTEXT COMMENT '正文全文；仅 owner_id 本人可检索，查询条件强制限定',
  `preview` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '列表预览，150 字',
  `owner_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '属主展示名，冗余避免列表 N+1',
  `update_time` DATETIME NOT NULL COMMENT '文件最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_note` (`owner_id`,`note_name`),
  KEY `idx_public_time` (`is_public`,`update_time`),
  KEY `idx_source` (`source_key`),
  KEY `idx_owner_time` (`owner_id`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记检索索引，md 文件为真相源';
