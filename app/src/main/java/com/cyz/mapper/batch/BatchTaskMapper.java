package com.cyz.mapper.batch;

import com.cyz.batch.model.BatchTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 批次任务 Mapper（batch_task）。 */
public interface BatchTaskMapper {

    List<BatchTask> listAll();

    BatchTask getById(@Param("id") Long id);

    /** 保存可编辑项（名称/目录/脚本/方式/计划/启用/超时）并写入下次执行时间 */
    int updateConfig(BatchTask task);

    /** 开始执行：置 running、记开始时间、次数+1 */
    int markRunning(@Param("id") Long id, @Param("runTime") String runTime);

    /** 执行结束：状态/退出码/耗时/日志尾部 */
    int markFinished(@Param("id") Long id, @Param("status") String status,
                     @Param("exitCode") Integer exitCode, @Param("durationMs") Long durationMs,
                     @Param("log") String log);

    int updateNextRun(@Param("id") Long id, @Param("nextRunTime") String nextRunTime);

    /** 到点待执行：enabled=1 且 next_run_time <= now */
    List<BatchTask> listDue(@Param("now") String now);

    /** 启用且有计划但下次执行时间为空（新建/启用后补算） */
    List<BatchTask> listMissingNextRun();
}
