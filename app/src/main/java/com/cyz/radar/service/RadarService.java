package com.cyz.radar.service;

import com.cyz.constant.ErrorEnum;
import com.cyz.mapper.mysqlMapper.RadarMapper;
import com.cyz.pojo.Result;
import com.cyz.push.PushService;
import com.cyz.radar.model.RadarSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 羊毛雷达：订阅管理 + 每小时增量推送。
 * <p>
 * 推送水位（last_post_id）语义：只有「整批推送成功」才推进水位；
 * 任一订阅推送失败保持原水位，下一轮自然重试（个人平台量小，不熔断）。
 */
@Service
@Slf4j
public class RadarService {

    private static final int MAX_SUBSCRIBE_PER_USER = 10;
    private static final int MAX_HITS_PER_RUN = 20;

    @Resource
    private RadarMapper radarMapper;

    @Resource
    private PushService pushService;

    @Value("${wxyd.app-name:wxyd}")
    private String appName;

    /* ---------------- 订阅管理 ---------------- */

    public List<RadarSubscribe> list(long userId) {
        return radarMapper.listByUser(userId);
    }

    /** 新增订阅；channel=dingtalk 校验 robotId 必填，channel=email 校验 email 格式 */
    public RadarSubscribe add(long userId, String keyword, String channel, Long robotId, String email) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty() || kw.length() > 64) {
            throw new IllegalArgumentException("关键词为空或超长（≤64 字符）");
        }
        String ch = channel == null ? "" : channel.trim();
        if (!"dingtalk".equals(ch) && !"email".equals(ch)) {
            throw new IllegalArgumentException("channel 非法（dingtalk | email）");
        }
        RadarSubscribe s = new RadarSubscribe();
        s.setUserId(userId);
        s.setKeyword(kw);
        s.setChannel(ch);
        if ("dingtalk".equals(ch)) {
            if (robotId == null) throw new IllegalArgumentException("钉钉通道必须选择机器人");
            s.setRobotId(robotId);
        } else {
            if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException("邮件通道必须填写有效邮箱");
            }
            s.setEmail(email.trim());
        }
        if (radarMapper.countByUser(userId) >= MAX_SUBSCRIBE_PER_USER) {
            throw new IllegalStateException("订阅数已达上限（" + MAX_SUBSCRIBE_PER_USER + " 条）");
        }
        // 新订阅以 scan_post 当前最大 id 为水位：只推未来新帖，不倒灌历史
        Long maxId = radarMapper.selectMaxPostId();
        s.setLastPostId(maxId == null ? 0L : maxId);
        s.setEnabled(1);
        try {
            radarMapper.insert(s);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("uk_user_keyword_channel")) {
                throw new IllegalStateException("相同关键词与通道的订阅已存在");
            }
            throw new IllegalStateException("保存失败");
        }
        return s;
    }

    public boolean delete(long userId, Long id) {
        return id != null && radarMapper.delete(id, userId) > 0;
    }

    public boolean toggle(long userId, Long id, boolean enabled) {
        return id != null && radarMapper.updateEnabled(id, userId, enabled ? 1 : 0) > 0;
    }

    /**
     * 立即测试推送：取该订阅关键词的「最新 1 条命中」（不看水位），推完不更新水位。
     * 用于配置后自测通道连通性。
     */
    public Result test(long userId, Long id) {
        Result result = Result.getInstance();
        RadarSubscribe s = id == null ? null : radarMapper.getById(id);
        if (s == null || s.getUserId() != userId) {
            return result.setErrorEnum(ErrorEnum.ERROR000014); // 无权操作（不泄露存在性）
        }
        List<Map<String, Object>> hits = radarMapper.findHits(s.getKeyword(), 0L, 1);
        String err;
        if (hits == null || hits.isEmpty()) {
            // 无历史命中也发一条确认消息，让用户知道通道是通的
            err = pushPlain(s, "【羊毛雷达·测试】订阅已生效，当前暂无命中「" + s.getKeyword() + "」的新帖。");
        } else {
            List<RadarSubscribe.HitPost> one = new ArrayList<>();
            one.add(toHit(hits.get(0)));
            err = pushHits(s, one, "【羊毛雷达·测试】");
        }
        if (err != null) {
            result.setErrorMsg("推送失败：" + err);
            result.setErrorCode("000015");
        } else {
            result.setBody("推送成功，请查收");
        }
        return result;
    }

    /* ---------------- 定时增量推送 ---------------- */

    @Scheduled(cron = "0 10 * * * *") // 每小时第 10 分，错开 dataScan 整点抓取
    public void pushJob() {
        List<RadarSubscribe> subs = radarMapper.listEnabled();
        if (subs == null || subs.isEmpty()) {
            log.info("[RADAR] tick：无启用订阅，跳过");
            return;
        }
        int pushed = 0, failed = 0;
        for (RadarSubscribe s : subs) {
            try {
                List<Map<String, Object>> rows = radarMapper.findHits(s.getKeyword(), s.getLastPostId(), MAX_HITS_PER_RUN);
                if (rows == null || rows.isEmpty()) continue;
                List<RadarSubscribe.HitPost> hits = new ArrayList<>();
                long maxId = s.getLastPostId() == null ? 0L : s.getLastPostId();
                for (Map<String, Object> r : rows) {
                    hits.add(toHit(r));
                    Object idv = r.get("id");
                    if (idv instanceof Number) maxId = Math.max(maxId, ((Number) idv).longValue());
                }
                String err = pushHits(s, hits, "【羊毛雷达】");
                if (err == null) {
                    radarMapper.updateLastPostId(s.getId(), maxId);
                    pushed++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.error("[RADAR] 订阅推送异常：id={} kw={}", s.getId(), s.getKeyword(), e);
            }
        }
        if (pushed > 0 || failed > 0) {
            log.info("[RADAR] 本轮完成：推送 {} 条订阅，失败 {}", pushed, failed);
        }
    }

    /* ---------------- 推送组装 ---------------- */

    private String pushHits(RadarSubscribe s, List<RadarSubscribe.HitPost> hits, String prefix) {
        StringBuilder md = new StringBuilder();
        md.append("### ").append(prefix).append("发现 ").append(hits.size()).append(" 条「").append(s.getKeyword()).append("」\n\n");
        for (RadarSubscribe.HitPost h : hits) {
            md.append("- **").append(h.getTitle() == null ? "(无标题)" : h.getTitle()).append("**")
              .append("  \n").append(h.getPublishedAt() == null ? "" : h.getPublishedAt());
            if (h.getSummary() != null && !h.getSummary().isEmpty()) {
                String sum = h.getSummary().length() > 120 ? h.getSummary().substring(0, 120) + "…" : h.getSummary();
                md.append("  \n").append(sum);
            }
            md.append("\n\n");
        }
        String mdText = md.toString();
        if ("dingtalk".equals(s.getChannel())) {
            return pushService.pushDingtalk(s.getUserId(), s.getRobotId(), "羊毛雷达：" + s.getKeyword(), mdText);
        }
        return pushService.pushEmail(s.getEmail(), "羊毛雷达：" + s.getKeyword() + "（" + hits.size() + " 条新帖）",
                mdText.replaceAll("### ", "").replaceAll("\\*\\*", "").replaceAll("  \n", "\n"));
    }

    /** 纯文本通知（测试通道用） */
    private String pushPlain(RadarSubscribe s, String text) {
        if ("dingtalk".equals(s.getChannel())) {
            return pushService.pushDingtalk(s.getUserId(), s.getRobotId(), "羊毛雷达", text);
        }
        return pushService.pushEmail(s.getEmail(), "羊毛雷达·测试", text);
    }

    @SuppressWarnings("unchecked")
    private static RadarSubscribe.HitPost toHit(Map<String, Object> row) {
        RadarSubscribe.HitPost h = new RadarSubscribe.HitPost();
        Object idv = row.get("id");
        if (idv instanceof Number) h.setId(((Number) idv).longValue());
        h.setTitle((String) row.get("title"));
        h.setSummary((String) row.get("summary"));
        h.setPublishedAt((String) row.get("publishedAt"));
        return h;
    }
}
