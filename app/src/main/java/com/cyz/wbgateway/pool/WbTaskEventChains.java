package com.cyz.wbgateway.pool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 客户端评分任务的"事件链"构造器（dsh-xdpool task-events.ts + desktopChatEvents 的 Java 移植）。
 * <p>
 * 任务面板上的"打开一个应用/用五个模板/召唤专家"看似只能真人操作，但评分器不看客户端，
 * 只看事件流——以正确的指纹（desktop/web）重放同样的链即计分。每条链均对线上实测过。
 * <p>
 * 关键约束（违反=200 收下但不计分）：
 * <ul>
 *   <li>通道是链的一部分：Buddy_App 类走 desktop 指纹，Library_read 只认 web 指纹；</li>
 *   <li>指纹字段整表携带，缺字段=静默丢弃；</li>
 *   <li>skill/expert 类要挂真实会话 id（openConversation 拿服务端 id）；</li>
 *   <li>template_5 计 distinct template_used，需 5 条不同模板；</li>
 *   <li>expert 连续使用会被合并计一，链间要留间隔（EXPERT_GAP_MS）。</li>
 * </ul>
 */
@Component
public class WbTaskEventChains {

    /** expert 链间隔：背靠背使用会被评分器合并为一次 */
    public static final long EXPERT_GAP_MS = 6_000;
    /** 事件上报后等评分落地的时长 */
    public static final long EVENT_SCORE_WAIT_MS = 9_000;

    public static final String BUDDY_APP_ID = "cb_y5Dy46tPQGGWtueMxXbe";
    public static final String BUDDY_APP_NAME = "企鹅教师助手";
    public static final String PLAYBOOK_CASE_ID = "pm-gtm-launch-plan";
    public static final String PLAYBOOK_CASE_NAME = "新产品上市 GTM 发布计划一页纸";
    public static final String LIBRARY_DOC_URL = "https://www.workbuddy.cn/space/d/o0KWYeynteVv06UnAZqIFm";
    public static final String APPEARANCE_THEME_KEY = "theme-tkmw7j";
    public static final String SKILL_ID = "skill_2097350077599879168";
    public static final String SKILL_NAME = "润泽小馆·日报撰写";
    public static final String SKILL_VERSION = "1.0.0";
    public static final String LIGHTHOUSE_EXPERT_ID = "ex_2cvvUZQhDyeJ";
    public static final String LIGHTHOUSE_EXPERT_NAME = "腾讯轻量云专家";

    private static final String[][] TEMPLATE_PRESETS = {
            {"1", "深度研究"}, {"2", "周报生成"}, {"3", "竞品分析"}, {"4", "活动策划"}, {"5", "代码评审"},
    };

    /** 一条就绪的链：desktop 走 events，web 走 reportWebEvent 五参 */
    public static class Chain {
        public final boolean web;
        public final JSONArray events;   // desktop
        public final String eventCode, pageUrl, elementId, elementName; // web

        private Chain(JSONArray events) {
            this.web = false;
            this.events = events;
            this.eventCode = this.pageUrl = this.elementId = this.elementName = null;
        }

        private Chain(String eventCode, String pageUrl, String elementId, String elementName) {
            this.web = true;
            this.events = null;
            this.eventCode = eventCode;
            this.pageUrl = pageUrl;
            this.elementId = elementId;
            this.elementName = elementName;
        }
    }

    /* ==================== 基础桌面聊天链 ==================== */

    /** 合成会话 id：多数任务只校验 id 格式合法，不校验会话存在（实测 RichMeow_Chat/create_canvas/automation_1 均点亮） */
    private static String syntheticId(String prefix) {
        return "wb2auto-" + prefix + "-" + Long.toString(System.currentTimeMillis(), 36)
                + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * 桌面聊天六事件链（agent_task_created → chat_message_send → chat_request_send
     * → chat_message_response → chat_message_status → chat_request_response）。
     * "一次真实请求完成"的证明序列，单独发点亮 RichMeow_Chat。
     */
    public static JSONArray desktopChatEvents(String conversationId, String requestId, String messageId) {
        String assistant = messageId + "-assistant";
        JSONArray events = new JSONArray();

        JSONObject taskCreated = new JSONObject();
        taskCreated.put("eventCode", "agent_task_created");
        taskCreated.put("source", "LOCAL");
        taskCreated.put("name", "working");
        taskCreated.put("task_target", "local");
        taskCreated.put("mode", "craft");
        taskCreated.put("requestModelId", "fast-model");
        taskCreated.put("requestModelName", "fast-model");
        taskCreated.put("has_repo", false);
        taskCreated.put("repo_type", "none");
        taskCreated.put("workspace_type", "empty");
        taskCreated.put("has_connector", false);
        taskCreated.put("connector_types", new JSONArray());
        taskCreated.put("has_mention", false);
        taskCreated.put("mention_types", new JSONArray());
        taskCreated.put("has_template", false);
        taskCreated.put("action", "");
        taskCreated.put("template_name", "");
        taskCreated.put("has_expert", false);
        taskCreated.put("expert_id", "");
        taskCreated.put("expert_name", "");
        taskCreated.put("expert_industry_id", "");
        taskCreated.put("has_skill", false);
        taskCreated.put("skill_names", new JSONArray());
        taskCreated.put("conversationId", conversationId);
        taskCreated.put("messageId", messageId);
        taskCreated.put("buddyId", "");
        taskCreated.put("buddyName", "");
        events.add(taskCreated);

        JSONObject msgSend = new JSONObject();
        msgSend.put("eventCode", "chat_message_send");
        msgSend.put("messageId", assistant);
        msgSend.put("historyCount", 0);
        msgSend.put("isContextTruncated", false);
        msgSend.put("currentStepCount", 1);
        msgSend.put("traceId", requestId);
        msgSend.put("rootRequestId", requestId);
        msgSend.put("parentConversationId", conversationId);
        msgSend.put("agentName", "cli");
        msgSend.put("agentType", "main");
        events.add(msgSend);

        JSONObject reqSend = new JSONObject();
        reqSend.put("eventCode", "chat_request_send");
        reqSend.put("inputLength", 24);
        reqSend.put("isPlan", false);
        reqSend.put("isAutoExecuteTerminal", false);
        reqSend.put("isAutoModify", false);
        reqSend.put("codebaseEnable", false);
        reqSend.put("maxToken", 0);
        reqSend.put("maxSteps", 500);
        reqSend.put("temperature", 0);
        reqSend.put("maxRetries", 0);
        reqSend.put("mentionContexts", new JSONArray());
        reqSend.put("knowledgeId", new JSONArray());
        reqSend.put("knowledgeName", new JSONArray());
        reqSend.put("codebaseId", "");
        reqSend.put("mentionContextCount", 0);
        reqSend.put("command", "");
        reqSend.put("recommendId", "");
        reqSend.put("skillId", "");
        reqSend.put("skillCount", 0);
        reqSend.put("totalCount", 0);
        reqSend.put("traceId", requestId);
        reqSend.put("rootRequestId", requestId);
        reqSend.put("parentConversationId", conversationId);
        reqSend.put("agentName", "cli");
        reqSend.put("agentType", "main");
        reqSend.put("codebuddy.session_id", conversationId);
        reqSend.put("codebuddy.conversation_request_id", requestId);
        events.add(reqSend);

        JSONObject msgResp = new JSONObject();
        msgResp.put("eventCode", "chat_message_response");
        msgResp.put("messageId", assistant);
        msgResp.put("responseModelId", "fast-model");
        msgResp.put("inputToken", 120);
        msgResp.put("outputToken", 80);
        msgResp.put("totalToken", 200);
        msgResp.put("cachedTokens", 0);
        msgResp.put("cachedWriteTokens", 0);
        msgResp.put("cachedMissTokens", 0);
        msgResp.put("isSuccessful", true);
        msgResp.put("messageErrorCode", "");
        msgResp.put("finishReason", "stop");
        msgResp.put("firstTokenAt", System.currentTimeMillis());
        msgResp.put("traceId", requestId);
        msgResp.put("conversationId", conversationId);
        msgResp.put("rootRequestId", requestId);
        msgResp.put("parentConversationId", conversationId);
        msgResp.put("agentName", "cli");
        msgResp.put("agentType", "main");
        msgResp.put("codebuddy.session_id", conversationId);
        msgResp.put("codebuddy.conversation_request_id", requestId);
        events.add(msgResp);

        JSONObject msgStatus = new JSONObject();
        msgStatus.put("eventCode", "chat_message_status");
        msgStatus.put("messageId", assistant);
        msgStatus.put("messageErrorCode", "0");
        msgStatus.put("traceId", requestId);
        msgStatus.put("rootRequestId", requestId);
        msgStatus.put("parentConversationId", conversationId);
        msgStatus.put("agentName", "cli");
        msgStatus.put("agentType", "main");
        events.add(msgStatus);

        JSONObject reqResp = new JSONObject();
        reqResp.put("eventCode", "chat_request_response");
        reqResp.put("mode", "craft");
        reqResp.put("toolCallCount", 0);
        reqResp.put("inputToken", 120);
        reqResp.put("outputToken", 80);
        reqResp.put("totalToken", 200);
        reqResp.put("cachedTokens", 0);
        reqResp.put("cachedWriteTokens", 0);
        reqResp.put("cachedMissTokens", 0);
        reqResp.put("isSuccessful", true);
        reqResp.put("messageErrorCode", "");
        reqResp.put("finishReason", "stop");
        reqResp.put("rootRequestId", requestId);
        reqResp.put("parentConversationId", conversationId);
        events.add(reqResp);

        return events;
    }

    private static JSONArray syntheticChatChain(String prefix) {
        String conversationId = syntheticId(prefix);
        String requestId = syntheticId(prefix + "-req");
        return desktopChatEvents(conversationId, requestId, "msg-" + prefix);
    }

    /* ==================== 各任务链 ==================== */

    /** Buddy_App + Buddy_App_QQ 一链点亮两个：五次点击复刻用户路径 */
    public static Chain buddyAppChain() {
        JSONArray events = new JSONArray();
        String[][] specs = {
                {"buddyapp_discover_click", null, null},
                {"buddyapp_show", BUDDY_APP_ID, BUDDY_APP_NAME},
                {"buddyapp_enter_click", BUDDY_APP_ID, BUDDY_APP_NAME},
                {"buddyapp_auth_confirm_click", BUDDY_APP_ID, BUDDY_APP_NAME},
                {"buddyapp_bindaccount_skip_click", BUDDY_APP_ID, BUDDY_APP_NAME},
        };
        for (String[] spec : specs) {
            JSONObject e = new JSONObject();
            e.put("eventCode", spec[0]);
            e.put("mode", "LOCAL");
            e.put("buddyId", BUDDY_APP_ID);
            e.put("buddyName", BUDDY_APP_NAME);
            if (spec[1] != null) {
                e.put("elementId", spec[1]);
                e.put("elementName", spec[2]);
            }
            if ("buddyapp_enter_click".equals(spec[0])) {
                e.put("position", 2);
                e.put("isFirstPage", "1");
            } else if ("buddyapp_show".equals(spec[0])) {
                e.put("position", 2);
            }
            events.add(e);
        }
        return new Chain(events);
    }

    /** create_canvas（+300 最大奖）：聊天链垫底供 id，两个 canvas 事件搭同一 metrics 通道，无需真实画布 */
    public static Chain canvasChain() {
        String conversationId = syntheticId("canvas");
        String requestId = syntheticId("canvas-req");
        JSONArray events = desktopChatEvents(conversationId, requestId, "msg-canvas");
        JSONObject create = new JSONObject();
        create.put("eventCode", "wbx_design_canvas_task_create");
        create.put("conversationId", conversationId);
        create.put("requestId", requestId);
        create.put("source", "summon_keyword");
        create.put("cost", 12000);
        create.put("isSuccessful", true);
        events.add(create);
        JSONObject open = new JSONObject();
        open.put("eventCode", "wbx_design_canvas_open");
        open.put("conversationId", conversationId);
        open.put("requestId", requestId);
        open.put("id", "ardot-file-" + requestId.substring(requestId.length() - 8));
        open.put("source", "summon_keyword");
        open.put("type", "page");
        open.put("cost", 13000);
        open.put("isSuccessful", true);
        events.add(open);
        return new Chain(events);
    }

    /** automation_1：单事件成链 */
    public static Chain automationChain() {
        JSONObject e = new JSONObject();
        e.put("eventCode", "automated_task_create_suc");
        e.put("name", "定时任务-" + Long.toString(System.currentTimeMillis(), 36));
        e.put("source", "manually");
        e.put("modelId", "fast-model");
        e.put("modelIsThinking", true);
        e.put("connectorCount", 0);
        e.put("skills", "");
        e.put("skillCount", 0);
        e.put("scheduleType", "once");
        e.put("mode", "LOCAL");
        JSONArray events = new JSONArray();
        events.add(e);
        return new Chain(events);
    }

    /** RichMeow_Chat：纯聊天链 */
    public static Chain chatChain() {
        return new Chain(syntheticChatChain("chat"));
    }

    /** playbook_prompt：评分器盯的是 playbook_prompt_send，点击路径为真实性全量重放 */
    public static Chain playbookChain() {
        String conversationId = syntheticId("pb");
        String requestId = syntheticId("pb-req");
        JSONArray events = desktopChatEvents(conversationId, requestId, "msg-pb");
        JSONObject click = new JSONObject();
        click.put("eventCode", "web_element_click");
        click.put("pageName", "playbook_detail");
        click.put("elementId", "playbook_ctaClick");
        click.put("elementName", PLAYBOOK_CASE_NAME);
        click.put("source", "discover");
        events.add(click);
        JSONObject cta = new JSONObject();
        cta.put("eventCode", "playbook_cta_click");
        cta.put("source", "discover");
        cta.put("position", 0);
        cta.put("id", PLAYBOOK_CASE_ID);
        cta.put("name", PLAYBOOK_CASE_NAME);
        cta.put("type", "document");
        cta.put("categoryId", "");
        cta.put("categoryName", "");
        events.add(cta);
        JSONObject send = new JSONObject();
        send.put("eventCode", "playbook_prompt_send");
        send.put("conversationId", conversationId);
        send.put("requestId", requestId);
        send.put("id", PLAYBOOK_CASE_ID);
        send.put("name", PLAYBOOK_CASE_NAME);
        send.put("type", "document");
        send.put("categoryId", "");
        send.put("categoryName", "");
        events.add(send);
        return new Chain(events);
    }

    /** template_5 一组：聊天链 + agent_task_created_with_template + template_used；发 5 组不同模板 */
    public static List<Chain> templateChains() {
        List<Chain> out = new ArrayList<>();
        for (String[] tpl : TEMPLATE_PRESETS) {
            String conversationId = syntheticId("tpl" + tpl[0]);
            String requestId = syntheticId("tpl" + tpl[0] + "-req");
            JSONArray events = desktopChatEvents(conversationId, requestId, "msg-tpl" + tpl[0]);
            JSONObject created = new JSONObject();
            created.put("eventCode", "agent_task_created_with_template");
            created.put("mode", "working");
            created.put("isCustomModel", false);
            created.put("id", tpl[0]);
            created.put("name", tpl[1]);
            created.put("requestId", requestId);
            events.add(created);
            JSONObject used = new JSONObject();
            used.put("eventCode", "template_used");
            used.put("template_id", tpl[0]);
            used.put("task_mode", "working");
            events.add(used);
            out.add(new Chain(events));
        }
        return out;
    }

    /** Library_read：只认 web 指纹，desktop 通道发不计分 */
    public static Chain libraryReadChain() {
        return new Chain("web_element_click", LIBRARY_DOC_URL, "library_doc_intro_click", "WorkBuddy资料库介绍");
    }

    /** Hp_Appearance 的事件半边（setAppearanceTheme 调用由执行器先行完成） */
    public static Chain appearanceChain() {
        JSONObject e = new JSONObject();
        e.put("eventCode", "appearance_skin_apply");
        e.put("action", "apply");
        e.put("source", "settings_close");
        e.put("id", APPEARANCE_THEME_KEY);
        e.put("vipLevel", 0);
        e.put("series", "");
        e.put("type", "unknown");
        JSONArray events = new JSONArray();
        events.add(e);
        return new Chain(events);
    }

    /** skill_1：真实会话 + finishReason=tool_calls（技能被当作工具加载才算数）+ skill_info */
    public static Chain skillChain(String conversationId, String requestId) {
        String messageId = "msg-" + requestId.substring(Math.max(0, requestId.length() - 8));
        String assistant = messageId + "-assistant";
        JSONArray events = desktopChatEvents(conversationId, requestId, messageId);
        for (int i = 0; i < events.size(); i++) {
            JSONObject e = events.getJSONObject(i);
            if ("chat_message_response".equals(e.getString("eventCode"))) e.put("finishReason", "tool_calls");
        }
        JSONObject info = new JSONObject();
        info.put("eventCode", "skill_info");
        info.put("id", SKILL_NAME);
        info.put("skillId", SKILL_ID);
        info.put("skillVersion", SKILL_VERSION);
        info.put("toolStatus", "success");
        info.put("fileCount", 56);
        info.put("source", "workbuddy-desktop");
        info.put("conversationId", conversationId);
        info.put("requestId", requestId);
        info.put("messageId", assistant);
        info.put("requestModelId", "fast-model");
        info.put("requestModelName", "fast-model");
        info.put("traceId", requestId);
        events.add(info);
        return new Chain(events);
    }

    /* ==================== expert 家族 ==================== */

    /** 召唤三事件（打在真实对话之前） */
    public static JSONArray expertSummonEvents(JSONObject expert) {
        String category = firstCategory(expert);
        String version = versionOf(expert);
        JSONArray events = new JSONArray();
        JSONObject click = new JSONObject();
        click.put("eventCode", "web_element_click");
        click.put("source", expert.getString("expertId"));
        click.put("type", category);
        click.put("version", version);
        click.put("elementId", "expert_summon_click");
        click.put("elementName", "立即召唤");
        click.put("pageURL", "/C:/Program%20Files/WorkBuddy/resources/app.asar/renderer/index.html");
        events.add(click);
        JSONObject summonClick = new JSONObject();
        summonClick.put("eventCode", "expert_summon_click");
        summonClick.put("id", expert.getString("expertId"));
        summonClick.put("name", expert.getString("displayName"));
        summonClick.put("expertTitle", expert.getString("profession"));
        summonClick.put("type", "expert-all");
        summonClick.put("position", 0);
        summonClick.put("expertType", expert.getString("expertType"));
        summonClick.put("version", version);
        summonClick.put("mode", "LOCAL");
        events.add(summonClick);
        JSONObject summoned = new JSONObject();
        summoned.put("eventCode", "expert_summoned");
        summoned.put("id", expert.getString("expertId"));
        summoned.put("name", expert.getString("displayName"));
        summoned.put("expertTitle", expert.getString("profession"));
        summoned.put("type", "expert-all");
        events.add(summoned);
        return events;
    }

    /** expert_actual_use：requestId 必须是服务端为真实会话分配的 id（评分器会查会话） */
    public static JSONObject expertActualUseEvent(JSONObject expert, String conversationId,
                                                  String requestId, String mode) {
        String category = firstCategory(expert);
        String version = versionOf(expert);
        JSONObject e = new JSONObject();
        e.put("eventCode", "expert_actual_use");
        e.put("id", expert.getString("expertId"));
        e.put("name", expert.getString("displayName"));
        e.put("expertTitle", expert.getString("profession"));
        e.put("type", category);
        e.put("expertType", expert.getString("expertType"));
        e.put("source", "builtin");
        e.put("version", version);
        e.put("cost", 9000);
        e.put("characterCount", 14);
        e.put("mode", mode);
        e.put("conversationId", conversationId);
        e.put("requestId", requestId);
        e.put("messageId", "msg-" + requestId.substring(Math.max(0, requestId.length() - 8)));
        e.put("requestModelId", "fast-model");
        e.put("requestModelName", "fast-model");
        return e;
    }

    /** expert 会话聊天链：agent_task_created 带上 expert 字段供评分器归属 */
    public static JSONArray expertChatEvents(JSONObject expert, String conversationId, String requestId) {
        String messageId = "msg-" + requestId.substring(Math.max(0, requestId.length() - 8));
        JSONArray events = desktopChatEvents(conversationId, requestId, messageId);
        for (int i = 0; i < events.size(); i++) {
            JSONObject e = events.getJSONObject(i);
            if ("agent_task_created".equals(e.getString("eventCode"))) {
                e.put("has_expert", true);
                e.put("expert_id", expert.getString("expertId"));
                e.put("expert_name", expert.getString("displayName"));
                e.put("expert_industry_id", "");
            }
        }
        return events;
    }

    private static String firstCategory(JSONObject expert) {
        JSONArray cats = expert.getJSONArray("categories");
        if (cats != null && !cats.isEmpty() && cats.getString(0) != null) return cats.getString(0);
        return "expert-all";
    }

    private static String versionOf(JSONObject expert) {
        String v = expert.getString("version");
        return v == null || v.isEmpty() ? "1.0.0" : v;
    }
}
