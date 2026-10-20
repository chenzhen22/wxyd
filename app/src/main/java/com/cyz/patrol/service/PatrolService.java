package com.cyz.patrol.service;

import com.cyz.mapper.mysqlMapper.PatrolMapper;
import com.cyz.patrol.model.PatrolReport;
import com.cyz.push.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 巡检日报：零配置系统自检（固定四项）+ 每日汇总推送。
 * <p>
 * 日报策略：每小时整点跑，命中 push_hour 的用户当日收到一条汇总；
 * 用户可在 patrol_setting 自选推送小时与通道。未配置 setting 的用户不推。
 * 最近一次巡检结果存内存（单实例语义，重启后需重新巡检）。
 */
@Service
@Slf4j
public class PatrolService {

    @Resource
    private PatrolMapper patrolMapper;

    @Resource
    private PatrolProbe patrolProbe;

    @Resource
    private PushService pushService;

    /** 最近一次巡检结果（volatile 单值，GET patrol/status 读） */
    private volatile PatrolReport lastReport;

    /* ---------------- 巡检执行 ---------------- */

    /** 跑一轮系统自检，更新内存中的最近结果并返回 */
    public PatrolReport runAll(long userId) {
        PatrolReport report = patrolProbe.runAll(userId);
        lastReport = report;
        return report;
    }

    /** 最近一次巡检结果（从未巡检过返回 null） */
    public PatrolReport getStatus() {
        return lastReport;
    }

    /* ---------------- 每日日报 ---------------- */

    /** 每小时整点：命中 push_hour 的用户推一条汇总日报 */
    @Scheduled(cron = "0 0 * * * *")
    public void dailyReportJob() {
        int hour = java.time.LocalTime.now().getHour();
        List<Map<String, Object>> users = patrolMapper.listUsersToPush(hour);
        if (users == null || users.isEmpty()) return;
        int ok = 0, failed = 0;
        for (Map<String, Object> u : users) {
            long userId = ((Number) u.get("user_id")).longValue();
            String channel = (String) u.get("channel");
            try {
                PatrolReport report = runAll(userId);
                String md = report.toMarkdown();
                String err;
                if ("dingtalk".equals(channel)) {
                    Number robotId = (Number) u.get("robot_id");
                    err = pushService.pushDingtalk(userId, robotId.longValue(), "巡检日报", md);
                } else {
                    err = pushService.pushEmail((String) u.get("email"),
                            "巡检日报：异常 " + report.getFailCount() + " / 警告 " + report.getWarnCount(),
                            md.replaceAll("### ", "").replaceAll("\\*\\*", ""));
                }
                if (err == null) ok++;
                else failed++;
            } catch (Exception e) {
                failed++;
                log.error("[PATROL] 日报推送异常：user={}", userId, e);
            }
        }
        if (ok > 0 || failed > 0) {
            log.info("[PATROL] 日报完成：推送 {} 人，失败 {}", ok, failed);
        }
    }

    /* ---------------- 测试推送 ---------------- */

    /**
     * 立即巡检并按当前日报设置推一条（不看 push_hour），用于配置后验证全链路。
     * 未保存/停用设置时返回原因而非沉默 —— 与定时任务按 push_hour 触发的语义区分开。
     *
     * @return null=推送成功；非 null=失败/不可推送原因
     */
    public String testPush(long userId) {
        Map<String, Object> setting = patrolMapper.getSetting(userId);
        if (setting == null) {
            return "尚未保存日报设置（推送时间/通道）";
        }
        Number enabled = (Number) setting.get("enabled");
        if (enabled == null || enabled.intValue() != 1) {
            return "日报设置已停用";
        }
        String channel = (String) setting.get("channel");
        PatrolReport report = runAll(userId);
        String md = report.toMarkdown();
        if ("dingtalk".equals(channel)) {
            Number robotId = (Number) setting.get("robot_id");
            if (robotId == null) return "钉钉通道未绑定机器人";
            return pushService.pushDingtalk(userId, robotId.longValue(), "巡检日报·测试", md);
        }
        String email = (String) setting.get("email");
        return pushService.pushEmail(email, "巡检日报·测试：异常 " + report.getFailCount()
                + " / 警告 " + report.getWarnCount(), md.replaceAll("### ", "").replaceAll("\\*\\*", ""));
    }

    /* ---------------- 设置 ---------------- */

    public Map<String, Object> getSetting(long userId) {
        return patrolMapper.getSetting(userId);
    }

    public void saveSetting(long userId, Integer pushHour, String channel, Long robotId, String email) {
        if (pushHour == null || pushHour < 0 || pushHour > 23) throw new IllegalArgumentException("推送小时需在 0-23");
        String ch = channel == null ? "" : channel.trim();
        if (!"dingtalk".equals(ch) && !"email".equals(ch)) throw new IllegalArgumentException("通道非法");
        if ("dingtalk".equals(ch) && robotId == null) throw new IllegalArgumentException("钉钉通道必须选择机器人");
        if ("email".equals(ch) && (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            throw new IllegalArgumentException("邮件通道必须填写有效邮箱");
        }
        patrolMapper.upsertSetting(userId, pushHour, ch, robotId, email, 1);
    }
}