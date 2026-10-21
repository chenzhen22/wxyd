-- V24: 批次任务（按目录执行脚本，支持手动/定时）
-- 全新安装由 DataInitializer 启动自检自动建表并播种 aiAgent 任务（本文件为手工执行备份）

CREATE TABLE IF NOT EXISTS `batch_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_code` VARCHAR(50) NOT NULL COMMENT '任务ID（唯一短码，也是日志文件名）',
  `name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '任务名称',
  `work_dir` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '任务目录（进程工作目录）',
  `script` VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '执行脚本/命令（bash -lc）',
  `exec_mode` VARCHAR(16) NOT NULL DEFAULT 'serial' COMMENT 'serial=串行（运行中跳过） parallel=允许并发',
  `plan` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '执行计划 cron（6 段），空=仅手动',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  `timeout_sec` INT NOT NULL DEFAULT 3600 COMMENT '超时秒数',
  `last_run_time` DATETIME NULL,
  `next_run_time` DATETIME NULL,
  `run_count` INT NOT NULL DEFAULT 0,
  `last_status` VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'ok|fail|timeout|running',
  `last_exit_code` INT NULL,
  `last_duration_ms` BIGINT NULL,
  `last_log` TEXT NULL COMMENT '最近一次执行日志尾部',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_next_run` (`enabled`,`next_run_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次任务';

-- 金融日报智能体：每天 08:00 在 /apps/GIT/aiAgent 执行 .venv/bin/python agent.py
INSERT IGNORE INTO `batch_task`
  (`task_code`,`name`,`work_dir`,`script`,`exec_mode`,`plan`,`enabled`,`timeout_sec`,`next_run_time`)
VALUES
  ('aiAgent','金融日报智能体','/apps/GIT/aiAgent','.venv/bin/python agent.py','serial','0 0 8 * * ?',1,3600,NULL),
  ('dataScan','福利羊毛抓取','/apps/GIT/dataScan','.venv/bin/python -m datascan.main --once','serial','0 */10 * * * ?',1,600,NULL);
