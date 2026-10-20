package com.cyz.patrol.model;

import lombok.Data;

import java.util.List;

/**
 * 系统自检巡检报告：固定四项（远程 MySQL / 应用自身 / 磁盘水位 / 推送通道），
 * 不落库 —— 最近一次结果存内存，由 GET patrol/status 下发。
 */
@Data
public class PatrolReport {

    /** 单项巡检结果 */
    @Data
    public static class CheckResult {
        private String name;
        /** ok | warn | fail | unknown */
        private String status;
        private String detail;
    }

    private List<CheckResult> results;
    private int failCount;
    private int warnCount;
    /** 本次巡检时间（yyyy-MM-dd HH:mm:ss） */
    private String lastRunTime;

    public String toMarkdown() {
        StringBuilder md = new StringBuilder();
        boolean allOk = failCount == 0 && warnCount == 0;
        if (allOk) {
            md.append("### ✅ 巡检日报\n\n").append(lastRunTime).append(" 全部 ")
              .append(results.size()).append(" 项系统自检正常。");
            for (CheckResult r : results) {
                md.append("\n- ✅ ").append(r.getName()).append("：").append(r.getDetail());
            }
            return md.toString();
        }
        md.append("### 🔔 巡检日报\n\n").append(lastRunTime).append("\n\n");
        if (failCount > 0) md.append("**❌ 异常 ").append(failCount).append(" 项**");
        if (warnCount > 0) {
            if (failCount > 0) md.append("，");
            md.append("**⚠️ 警告 ").append(warnCount).append(" 项**");
        }
        md.append("\n\n");
        for (CheckResult r : results) {
            if ("fail".equals(r.getStatus())) {
                md.append("- ❌ **").append(r.getName()).append("**：").append(r.getDetail()).append("\n");
            }
        }
        for (CheckResult r : results) {
            if ("warn".equals(r.getStatus())) {
                md.append("- ⚠️ **").append(r.getName()).append("**：").append(r.getDetail()).append("\n");
            }
        }
        md.append("\n---\n正常项：");
        for (CheckResult r : results) {
            if ("ok".equals(r.getStatus()) || "unknown".equals(r.getStatus())) {
                md.append(r.getName()).append("（").append("ok".equals(r.getStatus()) ? "✅" : "❔").append("） ");
            }
        }
        return md.toString();
    }
}