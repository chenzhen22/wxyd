package com.cyz.batch.model;

import lombok.Data;

/**
 * 批次任务（batch_task）：在服务器上按目录执行脚本，支持手动触发与 cron 定时触发。
 * <p>
 * 时间字段统一用 yyyy-MM-dd HH:mm:ss 字符串（与 wxyd 其他模块一致，SQL 里 date_format）。
 */
@Data
public class BatchTask {

    /** serial=串行（正在执行则跳过本次） parallel=允许并发 */
    public static final String MODE_SERIAL = "serial";
    public static final String MODE_PARALLEL = "parallel";

    private Long id;
    /** 任务ID（唯一短码，也是日志文件名） */
    private String taskCode;
    private String name;
    /** 任务目录（进程工作目录） */
    private String workDir;
    /** 执行脚本/命令（bash -lc 执行） */
    private String script;
    private String execMode;
    /** 6 段 cron（秒 分 时 日 月 周），空=仅手动执行 */
    private String plan;
    private Integer enabled;
    /** 超时秒数，超时强制结束 */
    private Integer timeoutSec;
    private String lastRunTime;
    private String nextRunTime;
    private Integer runCount;
    /** ok | fail | timeout | running | skip */
    private String lastStatus;
    private Integer lastExitCode;
    private Long lastDurationMs;
    /** 最近一次执行日志尾部（完整日志在 data/batch-task/<taskCode>.log） */
    private String lastLog;
    private String createTime;
}
