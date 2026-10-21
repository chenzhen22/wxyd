package com.cyz.batch.service;

import com.cyz.batch.model.BatchTask;
import com.cyz.mapper.batch.BatchTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 批次任务：按 cron 计划或手动触发，在指定目录执行脚本。
 * <p>
 * 调度：每 30 秒扫一次 batch_task（enabled=1 且 next_run_time<=now），命中即在后台线程执行，
 * 并先把 next_run_time 推进到下一次，避免重复触发。执行方式 serial 时同一任务不会并发。
 * 完整输出追加到 {@code data/batch-task/<taskCode>.log}，DB 只存尾部便于页面展示。
 */
@Service
@Slf4j
public class BatchTaskService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private BatchTaskMapper batchTaskMapper;

    @Value("${wxyd.batch.log-dir:data/batch-task}")
    private String logDir;

    /** 正在执行的任务（serial 模式用于跳过重入；parallel 不登记） */
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    private final ExecutorService pool = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "batch-task-runner");
        t.setDaemon(true);
        return t;
    });

    /* ==================== 查询 / 保存 ==================== */

    public List<BatchTask> list() {
        return batchTaskMapper.listAll();
    }

    /** 新建任务：任务ID 由系统生成（BT + 时间戳 + 3 位随机） */
    public BatchTask create(BatchTask in) {
        BatchTask t = new BatchTask();
        t.setTaskCode(generateCode());
        apply(t, in);
        batchTaskMapper.insert(t);
        log.info("[BATCH] 新建任务 {}（{}，plan={}）", t.getTaskCode(), t.getName(), t.getPlan());
        return batchTaskMapper.getById(t.getId());
    }

    /**
     * 保存可编辑项（任务ID 不可改）。plan 为空表示仅手动；否则必须是合法 cron。
     */
    public BatchTask save(BatchTask in) {
        if (in == null || in.getId() == null) throw new IllegalArgumentException("缺少任务 id");
        BatchTask cur = batchTaskMapper.getById(in.getId());
        if (cur == null) throw new IllegalArgumentException("任务不存在");
        apply(cur, in);
        batchTaskMapper.updateConfig(cur);
        log.info("[BATCH] 保存任务 {}（plan={}, enabled={}, next={}）",
                cur.getTaskCode(), cur.getPlan(), cur.getEnabled(), cur.getNextRunTime());
        return cur;
    }

    /** 校验入参并写回目标对象（任务ID 不参与修改） */
    private void apply(BatchTask target, BatchTask in) {
        if (in == null) throw new IllegalArgumentException("参数不能为空");
        String script = trim(in.getScript());
        if (script.isEmpty()) throw new IllegalArgumentException("执行脚本不能为空");
        String workDir = trim(in.getWorkDir());
        if (workDir.isEmpty()) throw new IllegalArgumentException("任务目录不能为空");
        if (!new File(workDir).isDirectory()) throw new IllegalArgumentException("任务目录不存在：" + workDir);
        String plan = normalizePlan(in.getPlan());
        if (!plan.isEmpty() && !CronExpression.isValidExpression(plan)) {
            throw new IllegalArgumentException("执行计划不是合法 cron：" + plan + "（示例：0 0 8 * * ? = 每天 08:00）");
        }
        String mode = BatchTask.MODE_PARALLEL.equalsIgnoreCase(trim(in.getExecMode()))
                ? BatchTask.MODE_PARALLEL : BatchTask.MODE_SERIAL;
        int timeout = in.getTimeoutSec() == null ? 3600 : in.getTimeoutSec();
        if (timeout < 1 || timeout > 86400) throw new IllegalArgumentException("超时秒数需在 1~86400 之间");
        int enabled = Integer.valueOf(1).equals(in.getEnabled()) ? 1 : 0;

        String name = trim(in.getName());
        target.setName(name.isEmpty() ? target.getTaskCode() : name);
        target.setWorkDir(workDir);
        target.setScript(script);
        target.setExecMode(mode);
        target.setPlan(plan);
        target.setEnabled(enabled);
        target.setTimeoutSec(timeout);
        target.setNextRunTime(enabled == 1 && !plan.isEmpty() ? nextRun(plan) : null);
    }

    /** 系统生成任务ID：BT + yyyyMMddHHmmss + 3 位随机（查重后使用） */
    private String generateCode() {
        for (int i = 0; i < 5; i++) {
            String code = "BT" + LocalDateTime.now().format(CODE_FMT)
                    + String.format("%03d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1000));
            if (batchTaskMapper.countByCode(code) == 0) return code;
        }
        throw new IllegalStateException("任务ID生成失败，请重试");
    }

    /* ==================== 执行 ==================== */

    /** 手动执行（异步，立即返回） */
    public void runNow(Long id) {
        BatchTask task = batchTaskMapper.getById(id);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        launch(task);
    }

    /** 提交到后台线程池；serial 模式运行中则抛异常由调用方提示 */
    private void launch(BatchTask task) {
        boolean serial = !BatchTask.MODE_PARALLEL.equalsIgnoreCase(task.getExecMode());
        if (serial && !running.add(task.getId())) {
            throw new IllegalStateException("任务正在执行中，请稍后再试");
        }
        pool.submit(() -> {
            try {
                execute(task);
            } catch (Exception e) {
                // execute 内部已回写失败状态；这里只记录，避免覆盖带日志尾部的状态
                log.error("[BATCH] 任务 {} 执行异常：{}", task.getTaskCode(), e.getMessage());
            } finally {
                if (serial) running.remove(task.getId());
            }
        });
    }

    /** 真正拉起子进程，输出重定向到日志文件 */
    private void execute(BatchTask task) throws Exception {
        long started = System.currentTimeMillis();
        String startedAt = LocalDateTime.now().format(FMT);
        batchTaskMapper.markRunning(task.getId(), startedAt);

        File logPath = logFile(task);
        Files.createDirectories(logPath.getParentFile().toPath());
        append(logPath, "\n===== " + startedAt + " 开始执行 =====" + System.lineSeparator()
                + "dir: " + task.getWorkDir() + System.lineSeparator()
                + "cmd: " + task.getScript() + System.lineSeparator());

        String status;
        Integer exitCode = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-lc", task.getScript());
            pb.directory(new File(task.getWorkDir()));
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logPath));
            Process process = pb.start();

            int timeout = task.getTimeoutSec() == null ? 3600 : task.getTimeoutSec();
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                status = "timeout";
            } else {
                exitCode = process.exitValue();
                status = exitCode == 0 ? "ok" : "fail";
            }
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - started;
            append(logPath, "[启动失败] " + e.getMessage() + System.lineSeparator());
            safeMarkFinished(task.getId(), "fail", null, ms, tail(logPath, 4000));
            throw e;
        }

        long ms = System.currentTimeMillis() - started;
        append(logPath, "===== " + LocalDateTime.now().format(FMT) + " 结束："
                + status + (exitCode == null ? "" : "（exit=" + exitCode + "）")
                + "，耗时 " + ms + "ms =====" + System.lineSeparator());
        batchTaskMapper.markFinished(task.getId(), status, exitCode, ms, tail(logPath, 4000));
        log.info("[BATCH] 任务 {} 执行完成 status={} exit={} 耗时={}ms", task.getTaskCode(), status, exitCode, ms);
    }

    /* ==================== 调度 ==================== */

    /** 每 30 秒扫一次到点任务；顺带补算缺失的 next_run_time */
    @Scheduled(fixedDelay = 30_000L, initialDelay = 20_000L)
    public void tick() {
        try {
            for (BatchTask t : batchTaskMapper.listMissingNextRun()) {
                batchTaskMapper.updateNextRun(t.getId(), nextRun(t.getPlan()));
            }
            String now = LocalDateTime.now().format(FMT);
            for (BatchTask t : batchTaskMapper.listDue(now)) {
                // 先推进下一次，避免执行期间被重复捞起
                batchTaskMapper.updateNextRun(t.getId(), nextRun(t.getPlan()));
                try {
                    launch(t);
                    log.info("[BATCH] 定时触发 {}（计划 {}）", t.getTaskCode(), t.getPlan());
                } catch (IllegalStateException e) {
                    log.warn("[BATCH] 任务 {} 上次仍在执行，本次计划触发跳过", t.getTaskCode());
                }
            }
        } catch (Exception e) {
            log.warn("[BATCH] 调度扫描失败：{}", e.getMessage());
        }
    }

    /** 计划归一：5 段（Linux crontab 写法）自动补秒位，变成 6 段 Spring cron */
    public static String normalizePlan(String plan) {
        String p = plan == null ? "" : plan.trim().replaceAll("\\s+", " ");
        if (p.isEmpty()) return "";
        return p.split(" ").length == 5 ? "0 " + p : p;
    }

    /** cron 下一次执行时间（无下一次返回 null） */
    public static String nextRun(String plan) {
        String p = normalizePlan(plan);
        if (p.isEmpty()) return null;
        try {
            LocalDateTime next = CronExpression.parse(p).next(LocalDateTime.now());
            return next == null ? null : next.format(FMT);
        } catch (Exception e) {
            return null;
        }
    }

    /* ==================== 日志 ==================== */

    /** 读取日志尾部（按行数） */
    public String tailLog(Long id, int lines) {
        BatchTask task = batchTaskMapper.getById(id);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        File log = logFile(task);
        if (!log.isFile()) return "（暂无日志，任务尚未执行）";
        String content = tail(log, 200_000);
        String[] arr = content.split("\\R");
        int from = Math.max(0, arr.length - Math.max(1, lines));
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < arr.length; i++) sb.append(arr[i]).append('\n');
        return sb.toString();
    }

    private File logFile(BatchTask task) {
        return new File(logDir, task.getTaskCode() + ".log");
    }

    private static void append(File file, String text) {
        try {
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.warn("[BATCH] 写日志失败 {}：{}", file, e.getMessage());
        }
    }

    /** 读文件末尾 maxChars 字符（大文件不全量读） */
    private static String tail(File file, int maxChars) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long len = raf.length();
            int size = (int) Math.min(len, maxChars);
            byte[] buf = new byte[size];
            raf.seek(len - size);
            raf.readFully(buf);
            return new String(buf, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "（读取日志失败：" + e.getMessage() + "）";
        }
    }

    private void safeMarkFinished(Long id, String status, Integer exitCode, Long durationMs, String logText) {
        try {
            batchTaskMapper.markFinished(id, status, exitCode, durationMs, logText);
        } catch (Exception e) {
            log.warn("[BATCH] 回写任务状态失败 id={}：{}", id, e.getMessage());
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
