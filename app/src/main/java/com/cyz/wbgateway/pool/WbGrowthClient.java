package com.cyz.wbgateway.pool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.wbgateway.WbUpstreamException;
import com.cyz.wbgateway.WbWorkBuddyClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 成长体系/积分自动化端点客户端（dsh-xdpool upstream.ts 成长域的 Java 移植）。
 * <p>
 * 路径备忘（实测核对，勿凭感觉改）：
 *  - 签到/补偿走 billingBase（国内 https://www.codebuddy.cn）；补偿路径【无 /v2】；
 *  - streak/lottery/buddy 走 chatBase 的 /activity/growth/*（【无 /v2】，带 /v2 不 404 但数据形状不对）；
 *  - 任务 list/accept 走 chatBase 的 /v2/activity/growth/*；
 *  - 任务领奖走 https://www.workbuddy.cn web origin + growth-center Origin/Referer + x-client-platform: web + 浏览器 UA。
 */
@Component
@Slf4j
public class WbGrowthClient {

    /** 重复签到标记（幂等成功） */
    private static final String[] ALREADY_CHECKIN_MARKERS = {"已签到", "already"};
    /** 无补偿可领标记（正常空态） */
    private static final String[] NO_COMPENSATION_MARKERS = {"没有可领取", "暂无可领取", "无可领取", "不可领取", "未开启", "没有活动"};

    @Value("${wxyd.wbgateway.client-version:5.5.6}")
    private String clientVersion;

    private OkHttpClient http;

    @Value("${wxyd.wbgateway.chat-timeout-seconds:300}")
    private int chatTimeoutSeconds;

    @Value("${wxyd.wbgateway.json-timeout-seconds:30}")
    private int jsonTimeoutSeconds;

    @Value("${wxyd.wbgateway.default-model:glm-5.3}")
    private String defaultModel;

    @Value("${wxyd.wbgateway.client-version:5.5.6}")
    private String cv;

    @javax.annotation.PostConstruct
    public void init() {
        http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(jsonTimeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    /* ==================== 基础 ==================== */

    private boolean isGlobal(WbPoolCredential c) {
        String d = c.getDomain() == null ? "" : c.getDomain().trim().toLowerCase();
        return d.equals("workbuddy.ai") || d.endsWith(".workbuddy.ai");
    }

    private String chatBase(WbPoolCredential c) {
        return isGlobal(c) ? "https://www.workbuddy.ai" : "https://copilot.tencent.com";
    }

    private String billingBase(WbPoolCredential c) {
        return isGlobal(c) ? "https://www.workbuddy.ai" : "https://www.codebuddy.cn";
    }

    /** 计费头（xdpool billingHeaders） */
    private Request.Builder billing(WbPoolCredential c, String url) {
        Request.Builder rb = new Request.Builder().url(url)
                .header("Authorization", "Bearer " + c.getAccessToken())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
        if (c.getUid() != null && !c.getUid().isEmpty()) rb.header("X-User-Id", c.getUid());
        if (c.getEnterpriseId() != null && !c.getEnterpriseId().isEmpty()) {
            rb.header("X-Enterprise-Id", c.getEnterpriseId());
            rb.header("X-Tenant-Id", c.getEnterpriseId());
        }
        if (c.getDomain() != null && !c.getDomain().isEmpty()) rb.header("X-Domain", c.getDomain());
        return rb;
    }

    /** chat 头（xdpool chatHeaders，含 X-No-* 约定与桌面 UA） */
    private Request.Builder chat(WbPoolCredential c, String url) {
        String ua = "WorkBuddy/" + clientVersion + " " + (isGlobal(c) ? "WorkBuddy AI" : "WorkBuddy") + "/" + clientVersion;
        Request.Builder rb = new Request.Builder().url(url)
                .header("Authorization", "Bearer " + c.getAccessToken())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Origin", "https://www.codebuddy.cn")
                .header("Referer", "https://www.codebuddy.cn/")
                .header("User-Agent", ua)
                .header(c.getUid() == null || c.getUid().isEmpty() ? "X-No-User-Id" : "X-User-Id",
                        c.getUid() == null || c.getUid().isEmpty() ? "1" : c.getUid())
                .header(c.getEnterpriseId() == null || c.getEnterpriseId().isEmpty() ? "X-No-Enterprise-Id" : "X-Enterprise-Id",
                        c.getEnterpriseId() == null || c.getEnterpriseId().isEmpty() ? "1" : c.getEnterpriseId())
                .header(c.getDomain() == null || c.getDomain().isEmpty() ? "X-No-Department-Info" : "X-Domain",
                        c.getDomain() == null || c.getDomain().isEmpty() ? "1" : c.getDomain())
                .header("X-IDE-Type", "WorkBuddy")
                .header("X-IDE-Name", "WorkBuddy")
                .header("X-IDE-Version", clientVersion)
                .header("X-Product", "SaaS");
        return rb;
    }

    /** growth 域统一信封调用：返回 data 对象 */
    private JSONObject growthJson(WbPoolCredential c, String method, String path, JSONObject body) throws Exception {
        Request.Builder rb = billing(c, chatBase(c) + path);
        if ("GET".equals(method)) rb.get();
        else rb.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                body == null ? "{}" : body.toJSONString()));
        return envelope(rb, method + " " + path);
    }

    /** 发请求并校验信封，返回 data */
    private JSONObject envelope(Request.Builder rb, String what) throws Exception {
        try (Response resp = http.newCall(rb.build()).execute()) {
            String text = resp.body() == null ? "" : resp.body().string();
            JSONObject envelope = null;
            try {
                envelope = JSON.parseObject(text);
            } catch (Exception ignore) {
                throw new WbUpstreamException(WbWorkBuddyClient.classify(resp.code(), text), resp.code(),
                        "HTTP " + resp.code() + " 非 JSON 响应");
            }
            if (!resp.isSuccessful() || envelope == null || envelope.getIntValue("code") != 0) {
                String msg = envelope == null ? "空响应" : envelope.getString("msg");
                throw new WbUpstreamException(WbWorkBuddyClient.classify(resp.code(), text), resp.code(),
                        WbWorkBuddyClient.extractDisplayErrorMessage(text) != null
                                ? WbWorkBuddyClient.extractDisplayErrorMessage(text)
                                : (msg == null ? what : msg));
            }
            return envelope.getJSONObject("data") == null ? new JSONObject() : envelope.getJSONObject("data");
        }
    }

    public boolean isAlreadyCheckin(Throwable e) {
        String m = String.valueOf(e.getMessage());
        for (String k : ALREADY_CHECKIN_MARKERS) if (m.contains(k)) return true;
        return false;
    }

    public boolean isNoCompensation(Throwable e) {
        String m = String.valueOf(e.getMessage()).toLowerCase();
        for (String k : NO_COMPENSATION_MARKERS) if (m.contains(k.toLowerCase())) return true;
        return false;
    }

    /* ==================== 签到 ==================== */

    /** 今日签到状态：active/todayCheckedIn/streakDays/dailyCredit 等 */
    public JSONObject checkinStatus(WbPoolCredential c) throws Exception {
        return envelope(billing(c, billingBase(c) + "/v2/billing/meter/checkin-activity-status")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{}")), "checkin-status");
    }

    /** 领取今日签到奖励，返回 {credit, streakDays} */
    public JSONObject claimDailyCheckin(WbPoolCredential c) throws Exception {
        return envelope(billing(c, billingBase(c) + "/v2/billing/meter/daily-checkin")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{}")), "daily-checkin");
    }

    /* ==================== 补偿 ==================== */

    /** 补偿状态（路径无 /v2） */
    public JSONObject compensationStatus(WbPoolCredential c) throws Exception {
        return envelope(billing(c, billingBase(c) + "/billing/meter/compensation-status")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{}")), "compensation-status");
    }

    public JSONObject claimCompensation(WbPoolCredential c) throws Exception {
        return envelope(billing(c, billingBase(c) + "/billing/meter/claim-compensation")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{}")), "claim-compensation");
    }

    /* ==================== 活跃上报 ==================== */

    /** 发一条 chat_request_send 桌面事件（点亮连登/热力图），照 xdpool reportActivity 的固定形状 */
    public void reportActivity(WbPoolCredential c) throws Exception {
        long now = System.currentTimeMillis();
        String cid = "wb2api-" + now;
        JSONObject event = new JSONObject();
        event.put("eventCode", "chat_request_send");
        event.put("timestamp", now);
        event.put("reportDelay", 0);
        event.put("mode", "craft");
        event.put("conversationId", cid);
        event.put("requestId", cid);
        event.put("inputLength", 12);
        event.put("requestModelId", "deepseek-v4-flash");
        event.put("requestModelName", "DeepSeek V4 Flash");
        event.put("isPlan", false);
        event.put("isAutoExecuteTerminal", false);
        event.put("isAutoModify", false);
        event.put("codebaseEnable", false);
        event.put("maxToken", 0);
        event.put("maxSteps", 0);
        event.put("temperature", 0);
        event.put("maxRetries", 0);
        event.put("mentionContexts", new JSONArray());
        event.put("knowledgeId", new JSONArray());
        event.put("knowledgeName", new JSONArray());
        event.put("codebaseId", "");
        event.put("mentionContextCount", 0);
        event.put("command", "");
        event.put("expertId", "");
        event.put("recommendId", "");
        event.put("skillId", "");
        event.put("skillCount", 0);
        event.put("totalCount", 0);
        event.put("fileUri", "");
        event.put("presentAt", now);
        event.put("traceId", "");
        event.put("rootRequestId", cid);
        event.put("parentConversationId", cid);
        event.put("agentName", "default");
        event.put("agentType", "conversation");
        event.put("userId", c.getUid() == null ? "" : c.getUid());
        JSONArray events = new JSONArray();
        events.add(event);
        envelope(billing(c, billingBase(c) + "/v2/report")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), events.toJSONString())), "report");
    }

    /* ==================== 成长体系（/activity/growth/*，无 /v2） ==================== */

    /** 连登天数（data.streak.days） */
    public int growthStreakDays(WbPoolCredential c) throws Exception {
        JSONObject data = growthJson(c, "GET", "/activity/growth/streak", null);
        JSONObject streak = data.getJSONObject("streak");
        return streak == null ? 0 : streak.getIntValue("days");
    }

    /** 连登全档位状态：data.streak + data.redemption_status.tiers[]（tier_{name}_status 为解锁态） */
    public JSONObject growthStreakFull(WbPoolCredential c) throws Exception {
        return growthJson(c, "GET", "/activity/growth/streak", null);
    }

    public void redeemStreakTier(WbPoolCredential c, String tier) throws Exception {
        JSONObject body = new JSONObject();
        body.put("tier", tier);
        body.put("client_token", UUID.randomUUID().toString());
        growthJson(c, "POST", "/activity/growth/redeem", body);
    }

    public int lotteryChances(WbPoolCredential c) throws Exception {
        return growthJson(c, "GET", "/activity/growth/lottery/summary", null).getIntValue("chances");
    }

    public JSONObject lotteryDraw(WbPoolCredential c) throws Exception {
        JSONObject body = new JSONObject();
        body.put("client_token", UUID.randomUUID().toString());
        return growthJson(c, "POST", "/activity/growth/lottery/draw", body);
    }

    /** buddy 档案；未领养返回 null */
    public JSONObject buddyInfo(WbPoolCredential c) throws Exception {
        JSONObject data = growthJson(c, "GET", "/activity/growth/buddy/info", null);
        JSONObject buddy = data.getJSONObject("buddy");
        if (buddy == null || buddy.isEmpty()) return null;
        return buddy;
    }

    public void buddyAgree(WbPoolCredential c) throws Exception {
        JSONObject body = new JSONObject();
        body.put("agree", true);
        growthJson(c, "POST", "/activity/growth/buddy/agreement", body);
    }

    public void buddyAdoptFirst(WbPoolCredential c) throws Exception {
        growthJson(c, "POST", "/activity/growth/buddy/first", new JSONObject());
    }

    public JSONObject buddyTravelStatus(WbPoolCredential c) throws Exception {
        return growthJson(c, "GET", "/activity/growth/buddy/travel/status", null);
    }

    public void buddyTravelDepart(WbPoolCredential c) throws Exception {
        JSONObject body = new JSONObject();
        body.put("location_id", 4); // 古镇客栈：四个地点收益/时长同分布，无可优化
        growthJson(c, "POST", "/activity/growth/buddy/travel/depart", body);
    }

    public int buddyTravelClaim(WbPoolCredential c, long recordId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("record_id", recordId);
        return growthJson(c, "POST", "/activity/growth/buddy/travel/claim", body).getIntValue("reward_credit");
    }

    /* ==================== 任务中心 ==================== */

    /** 任务列表（/v2/activity/growth/tasks，chat 头） */
    public JSONArray listTasks(WbPoolCredential c) throws Exception {
        JSONObject data = envelope(chat(c, chatBase(c) + "/v2/activity/growth/tasks").get(), "list-tasks");
        JSONArray tasks = data.getJSONArray("tasks");
        return tasks == null ? new JSONArray() : tasks;
    }

    public void acceptTasks(WbPoolCredential c, List<String> codes) throws Exception {
        if (codes.isEmpty()) return;
        JSONObject body = new JSONObject();
        body.put("task_codes", codes);
        envelope(chat(c, chatBase(c) + "/v2/activity/growth/tasks/accept")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toJSONString())), "accept-tasks");
    }

    /** 领取任务奖励：web origin 专属头；already_claimed 视为 {0,0} */
    public JSONObject claimTaskReward(WbPoolCredential c, String taskCode) throws Exception {
        Request.Builder rb = billing(c, "https://www.workbuddy.cn/activity/growth/tasks/"
                + taskCode + "/claim")
                .header("Accept", "application/json, text/plain, */*")
                .header("Origin", "https://www.workbuddy.cn")
                .header("Referer", "https://www.workbuddy.cn/profile/growth-center")
                .header("x-client-platform", "web")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new byte[0]));
        JSONObject data = envelope(rb, "claim-task");
        if (data.getBooleanValue("already_claimed")) {
            JSONObject zero = new JSONObject();
            zero.put("credit", 0);
            zero.put("energy", 0);
            zero.put("already_claimed", true);
            return zero;
        }
        return data;
    }
}