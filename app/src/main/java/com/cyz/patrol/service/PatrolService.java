package com.cyz.patrol.service;

import com.cyz.mapper.mysqlMapper.PatrolMapper;
import com.cyz.patrol.model.PatrolAsset;
import com.cyz.push.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 巡检日报：资产管理 + 探测编排 + 每日汇总推送。
 * <p>
 * 日报策略：PatrolJob 每小时整点跑，命中 push_hour 的用户当日收到一条汇总；
 * 用户可在 patrol_setting 自选推送小时与通道。未配置 setting 的用户不推（页面看 last_status）。
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

    /* ---------------- 资产管理 ---------------- */

    public List<PatrolAsset> list(long userId) {
        return patrolMapper.listByUser(userId);
    }

    /** 新增资产；按类型校验 + ssh_password SM4 加密入库 */
    public PatrolAsset add(long userId, String name, String assetType, String target,
                           Integer port, String sshUser, String sshPassword) {
        String n = name == null ? "" : name.trim();
        String t = target == null ? "" : target.trim();
        String type = assetType == null ? "" : assetType.trim();
        if (n.isEmpty() || n.length() > 64) throw new IllegalArgumentException("资产名为空或超长");
        if (t.isEmpty() || t.length() > 255) throw new IllegalArgumentException("探测目标为空或超长");
        PatrolAsset a = new PatrolAsset();
        a.setUserId(userId);
        a.setName(n);
        a.setAssetType(type);
        a.setTarget(t);
        a.setEnabled(1);
        switch (type) {
            case "url":
                if (!t.matches("^https?://.+")) throw new IllegalArgumentException("URL 需以 http(s):// 开头");
                break;
            case "domain":
                if (!t.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) throw new IllegalArgumentException("域名格式非法");
                break;
            case "server":
                if (sshUser == null || sshUser.trim().isEmpty()) throw new IllegalArgumentException("SSH 通道必须填写用户名");
                if (sshPassword == null || sshPassword.isEmpty()) throw new IllegalArgumentException("SSH 通道必须填写密码");
                a.setSshUser(sshUser.trim());
                a.setPort(port == null ? 22 : port);
                // SM4 密文入库（与 ding_robot.secret 同待遇），永不明文落库
                a.setSshPassword(sm4Encrypt(sshPassword));
                break;
            default:
                throw new IllegalArgumentException("资产类型非法（url | domain | server）");
        }
        try {
            patrolMapper.insert(a);
        } catch (Exception e) {
            log.error("[PATROL] 保存资产失败", e);
            throw new IllegalStateException("保存失败");
        }
        return a;
    }

    public boolean delete(long userId, Long id) {
        return id != null && patrolMapper.delete(id, userId) > 0;
    }

    /* ---------------- 巡检执行 ---------------- */

    /** 巡检一个用户全部启用的资产，更新 last_* 并返回结果 */
    public PatrolAsset.PatrolReport runForUser(long userId) {
        List<PatrolAsset> assets = patrolMapper.listEnabledByUser(userId);
        PatrolAsset.PatrolReport report = new PatrolAsset.PatrolReport();
        report.setUserId(userId);
        List<PatrolAsset.CheckResult> results = new ArrayList<>();
        int fail = 0, warn = 0;
        if (assets != null) {
            for (PatrolAsset a : assets) {
                PatrolAsset.CheckResult r = patrolProbe.check(a);
                results.add(r);
                if ("fail".equals(r.getStatus())) fail++;
                else if ("warn".equals(r.getStatus())) warn++;
                try {
                    patrolMapper.updateCheckResult(a.getId(), r.getStatus(), r.getDetail());
                } catch (Exception e) {
                    log.error("[PATROL] 巡检结果落库失败：asset {}", a.getId(), e);
                }
            }
        }
        report.setResults(results);
        report.setFailCount(fail);
        report.setWarnCount(warn);
        return report;
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
                PatrolAsset.PatrolReport report = runForUser(userId);
                if (report.getResults().isEmpty()) continue; // 无资产不打扰
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
     * 无资产时返回提示而非沉默 —— 与定时任务"无资产不打扰"的静默语义区分开。
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
        PatrolAsset.PatrolReport report = runForUser(userId);
        if (report.getResults().isEmpty()) {
            return "没有任何巡检资产，先添加资产（URL/域名/服务器）再测试";
        }
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

    @Resource
    private com.cyz.config.Sm4KeyHolder sm4KeyHolder;

    private String sm4Encrypt(String plain) {
        return sm4KeyHolder.encrypt(plain);
    }
}
