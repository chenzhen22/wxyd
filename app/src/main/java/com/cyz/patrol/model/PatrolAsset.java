package com.cyz.patrol.model;

import lombok.Data;

import java.util.List;

/**
 * 巡检资产（patrol_asset 表）：url 存活 / domain WHOIS 到期 / server SSH 磁盘内存。
 */
@Data
public class PatrolAsset {

    private Long id;
    private Long userId;
    private String name;
    /** url | domain | server */
    private String assetType;
    /** url:完整URL / domain:域名 / server:host */
    private String target;
    /** server 的 SSH 端口，默认 22 */
    private Integer port;
    private String sshUser;
    /** SM4 密文，永不下发前端 */
    private String sshPassword;
    private Integer enabled;
    /** ok | warn | fail | unknown */
    private String lastStatus;
    private String lastDetail;
    private String lastCheckTime;
    private String createTime;

    /** 巡检结果（runNow / 日报用，不落库） */
    @Data
    public static class CheckResult {
        private Long assetId;
        private String name;
        private String assetType;
        private String target;
        /** ok | warn | fail | unknown */
        private String status;
        private String detail;
    }

    /** 日报聚合（单用户单次巡检） */
    @Data
    public static class PatrolReport {
        private Long userId;
        private List<CheckResult> results;
        private int failCount;
        private int warnCount;

        public String toMarkdown() {
            StringBuilder md = new StringBuilder();
            boolean allOk = failCount == 0 && warnCount == 0;
            if (allOk) {
                md.append("### ✅ 巡检日报\n\n全部 ").append(results.size()).append(" 项资产状态正常。");
                for (CheckResult r : results) {
                    md.append("\n- ✅ ").append(r.getName()).append("：").append(r.getDetail());
                }
                return md.toString();
            }
            md.append("### 🔔 巡检日报\n\n");
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
                    md.append(r.getName()).append("（").append(r.getStatus().equals("ok") ? "✅" : "❔").append("） ");
                }
            }
            return md.toString();
        }
    }
}
