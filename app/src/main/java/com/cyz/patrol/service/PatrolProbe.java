package com.cyz.patrol.service;

import com.cyz.mapper.mysqlMapper.PatrolMapper;
import com.cyz.patrol.model.PatrolReport;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统自检探测器（零配置固定四项）：远程 MySQL 健康 / 应用自身 / 磁盘水位 / 推送通道。
 * 全部纯 Java 实现；任何异常都归一化为 CheckResult（fail），不向上抛。
 */
@Service
@Slf4j
public class PatrolProbe {

    /** SELECT 1 延迟超此值报警告（ms） */
    private static final long MYSQL_SLOW_MS = 500;
    /** 堆内存使用率告警阈值（%） */
    private static final int HEAP_WARN_PCT = 80;
    /** 磁盘已用率告警阈值（%） */
    private static final int DISK_WARN_PCT = 85;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    @Qualifier("mysqlDataSource")
    private DataSource dataSource;

    @Resource
    private PatrolMapper patrolMapper;

    /** SMTP 未配置时为空串（与 MailService 同源 env 配置） */
    @Value("${wxyd.smtp.host:}")
    private String smtpHost;

    /** 跑一轮全部固定巡检项，聚合为报告（含计数与时间戳） */
    public PatrolReport runAll(long userId) {
        List<PatrolReport.CheckResult> results = new ArrayList<>();
        results.add(checkMysql());
        results.add(checkJvm());
        results.add(checkDisk());
        results.add(checkChannel(userId));
        PatrolReport report = new PatrolReport();
        report.setResults(results);
        int fail = 0, warn = 0;
        for (PatrolReport.CheckResult r : results) {
            if ("fail".equals(r.getStatus())) fail++;
            else if ("warn".equals(r.getStatus())) warn++;
        }
        report.setFailCount(fail);
        report.setWarnCount(warn);
        report.setLastRunTime(LocalDateTime.now().format(TS));
        return report;
    }

    /** 远程 MySQL：SELECT 1 实测延迟 + HikariCP 池状态 + 版本/uptime/Threads_connected */
    public PatrolReport.CheckResult checkMysql() {
        PatrolReport.CheckResult r = item("远程 MySQL");
        try {
            long start = System.currentTimeMillis();
            try (Connection conn = dataSource.getConnection();
                 Statement st = conn.createStatement()) {
                st.executeQuery("SELECT 1");
                long cost = System.currentTimeMillis() - start;

                StringBuilder d = new StringBuilder("延迟 ").append(cost).append("ms");
                appendPoolStats(d);
                appendServerInfo(st, d); // 版本/uptime/连接数：global_status 无权限时静默降级
                r.setDetail(d.toString());
                r.setStatus(cost > MYSQL_SLOW_MS ? "warn" : "ok");
            }
        } catch (Exception e) {
            log.warn("[PATROL] MySQL 巡检失败：{}", e.getMessage());
            r.setStatus("fail");
            r.setDetail("连接失败：" + brief(e.getMessage()));
        }
        return r;
    }

    /** 版本 / uptime / Threads_connected（performance_schema 无权限时静默跳过） */
    private void appendServerInfo(Statement st, StringBuilder d) {
        try {
            String version = "";
            long uptime = -1, threads = -1;
            try (ResultSet rs = st.executeQuery(
                    "SELECT VERSION() AS v, " +
                    "(SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME='Uptime') AS up, " +
                    "(SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME='Threads_connected') AS tc")) {
                if (rs.next()) {
                    version = rs.getString(1);
                    uptime = rs.getLong(2);
                    threads = rs.getLong(3);
                }
            }
            if (!version.isEmpty()) d.append(" · MySQL ").append(version);
            if (uptime >= 0) d.append(" · 已运行 ").append(fmtUptime(uptime * 1000));
            if (threads >= 0) d.append(" · 连接数 ").append(threads);
        } catch (Exception e) {
            log.debug("[PATROL] 服务器信息读取失败（不影响判定）：{}", e.getMessage());
        }
    }

    /** HikariCP 活跃/空闲/等待（池未启动或非 Hikari 实现时静默跳过） */
    private void appendPoolStats(StringBuilder d) {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
                if (pool != null) {
                    d.append(" · 池 活跃").append(pool.getActiveConnections())
                     .append("/空闲").append(pool.getIdleConnections())
                     .append("/等待").append(pool.getThreadsAwaitingConnection());
                }
            }
        } catch (Exception e) {
            log.debug("[PATROL] 池状态读取失败（不影响判定）：{}", e.getMessage());
        }
    }

    /** 应用自身：JVM 运行时长 + 堆使用率 + CPU 负载 */
    public PatrolReport.CheckResult checkJvm() {
        PatrolReport.CheckResult r = item("应用自身");
        try {
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = mem.getHeapMemoryUsage();
            long max = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
            int heapPct = (int) (heap.getUsed() * 100 / max);

            StringBuilder d = new StringBuilder("已运行 ").append(fmtUptime(uptimeMs))
                    .append(" · 堆 ").append(fmtGb(heap.getUsed())).append("/").append(fmtGb(max))
                    .append("（").append(heapPct).append("%）");
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double cpu = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
                if (cpu >= 0) d.append(" · CPU ").append(Math.round(cpu * 100)).append("%");
            }
            double load = osBean.getSystemLoadAverage();
            if (load >= 0) d.append(" · load ").append(String.format("%.2f", load));

            r.setDetail(d.toString());
            r.setStatus(heapPct > HEAP_WARN_PCT ? "warn" : "ok");
        } catch (Exception e) {
            r.setStatus("fail");
            r.setDetail("读取失败：" + brief(e.getMessage()));
        }
        return r;
    }

    /** 磁盘水位：应用工作目录所在盘 */
    public PatrolReport.CheckResult checkDisk() {
        PatrolReport.CheckResult r = item("磁盘水位");
        try {
            File dir = new File(System.getProperty("user.dir"));
            long total = dir.getTotalSpace();
            long usable = dir.getUsableSpace();
            if (total <= 0) {
                r.setStatus("unknown");
                r.setDetail("无法获取磁盘空间（" + brief(dir.getAbsolutePath()) + "）");
                return r;
            }
            long used = total - usable;
            int pct = (int) (used * 100 / total);
            r.setDetail("工作目录所在盘 已用 " + pct + "%（余 " + fmtGb(usable) + " / 共 " + fmtGb(total) + "）");
            r.setStatus(pct >= DISK_WARN_PCT ? "warn" : "ok");
        } catch (Exception e) {
            r.setStatus("fail");
            r.setDetail("读取失败：" + brief(e.getMessage()));
        }
        return r;
    }

    /** 推送通道：SMTP 配置 + 日报设置绑定的钉钉机器人/邮箱有效性 */
    public PatrolReport.CheckResult checkChannel(long userId) {
        PatrolReport.CheckResult r = item("推送通道");
        try {
            boolean smtpOk = smtpHost != null && !smtpHost.trim().isEmpty();
            StringBuilder d = new StringBuilder("SMTP ").append(smtpOk ? "已配置" : "未配置");
            r.setStatus("ok");

            java.util.Map<String, Object> setting = patrolMapper.getSetting(userId);
            if (setting == null) {
                d.append(" · 日报推送未保存设置");
            } else {
                Number enabled = (Number) setting.get("enabled");
                String channel = (String) setting.get("channel");
                if (enabled == null || enabled.intValue() != 1) {
                    d.append(" · 日报推送已停用");
                } else if ("dingtalk".equals(channel)) {
                    Number robotId = (Number) setting.get("robot_id");
                    if (robotId == null) {
                        d.append(" · 钉钉通道未绑定机器人");
                        r.setStatus("warn");
                    } else if (patrolMapper.countRobot(userId, robotId.longValue()) > 0) {
                        d.append(" · 钉钉通道 · 机器人#").append(robotId).append(" 有效");
                    } else {
                        d.append(" · 绑定的机器人#").append(robotId).append("已不存在");
                        r.setStatus("warn");
                    }
                } else if ("email".equals(channel)) {
                    String email = (String) setting.get("email");
                    if (email == null || email.trim().isEmpty()) {
                        d.append(" · 邮件通道未填邮箱");
                        r.setStatus("warn");
                    } else if (!smtpOk) {
                        d.append(" · 收件 ").append(email).append("，但 SMTP 未配置");
                        r.setStatus("warn");
                    } else {
                        d.append(" · 邮件通道 · ").append(email).append(" 有效");
                    }
                } else {
                    d.append(" · 日报通道非法（").append(channel).append("）");
                    r.setStatus("warn");
                }
            }
            r.setDetail(d.toString());
        } catch (Exception e) {
            r.setStatus("fail");
            r.setDetail("读取失败：" + brief(e.getMessage()));
        }
        return r;
    }

    /* ---------------- 工具 ---------------- */

    private static PatrolReport.CheckResult item(String name) {
        PatrolReport.CheckResult r = new PatrolReport.CheckResult();
        r.setName(name);
        r.setStatus("unknown");
        return r;
    }

    /** ms → 「3天2小时5分」/「18分」/「42秒」 */
    private static String fmtUptime(long ms) {
        long sec = ms / 1000;
        long d = sec / 86400;
        long h = sec % 86400 / 3600;
        long m = sec % 3600 / 60;
        if (d > 0) return d + "天" + h + "小时" + m + "分";
        if (h > 0) return h + "小时" + m + "分";
        if (m > 0) return m + "分";
        return sec + "秒";
    }

    private static String fmtGb(long bytes) {
        double gb = bytes / 1073741824.0;
        if (gb >= 1) return String.format("%.1fGB", gb);
        return String.format("%.0fMB", bytes / 1048576.0);
    }

    private static String brief(String msg) {
        if (msg == null) return "";
        return msg.length() > 120 ? msg.substring(0, 120) : msg;
    }
}