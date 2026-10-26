package com.cyz.wbgateway.pool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.wbgateway.WbUpstreamException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 积分自动化（dsh-xdpool scheduler.ts 的 Java 移植，核心版：不做事件链合成）。
 * <p>
 * 每分钟整点唤醒比对各任务配置小时，lastRunDate != 今天 才跑（每任务每日一次）；
 * 手动「立即执行」不消耗当日档位。账号串行、间隔 800ms；单账号失败跳过继续；
 * 仅 CN 账号参与（global 无成长体系），会话死号跳过。配置与账本持久化 automation.json。
 */
@Service
@Slf4j
public class WbCreditAutomationService {

    private static final List<String> KINDS = Arrays.asList("checkin", "report", "events", "tasks", "streak", "travel", "compensation");
    private static final long ACCOUNT_DELAY_MS = 800;
    /** 每次领奖之间的间隔（上游对连发敏感） */
    private static final long CLAIM_DELAY_MS = 800;

    private final WbAccountPool accountPool;
    private final WbGrowthClient growth;
    private final WbTaskEventChains chains;
    private final com.cyz.wbgateway.WbWorkBuddyClient client;

    private boolean enabled = true;
    private final Map<String, int[]> hours = new LinkedHashMap<>();
    private final Map<String, String> lastRunDate = new LinkedHashMap<>();
    private final Map<String, JobState> states = new LinkedHashMap<>();
    /** accountId -> (date -> 各项收益) 简易账本 */
    private final Map<String, Map<String, Map<String, Long>>> earnings = new ConcurrentHashMap<>();

    private final Object lock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public WbCreditAutomationService(WbAccountPool accountPool, WbGrowthClient growth, WbTaskEventChains chains,
                                     com.cyz.wbgateway.WbWorkBuddyClient client) {
        this.accountPool = accountPool;
        this.growth = growth;
        this.chains = chains;
        this.client = client;
    }

    @Data
    public static class JobState {
        private String kind;
        private String lastRunDate = "";
        private long lastRunAtMs;
        private int ok;
        private int failed;
        private long credit;
        private long energy;
        private int claimed;
        private String message = "尚未运行";
        private List<String> detail = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        for (String k : KINDS) {
            states.put(k, stateOf(k));
            lastRunDate.put(k, "");
        }
        hours.put("checkin", new int[]{9});
        hours.put("report", new int[]{10});
        // events 在 tasks 之前跑：事件链计分要几秒落地，随后 tasks 的领取批次当场收奖
        hours.put("events", new int[]{10, 11});
        hours.put("tasks", new int[]{11});
        hours.put("streak", new int[]{12});
        hours.put("travel", new int[]{9, 21});
        hours.put("compensation", new int[]{11});
        loadConfig();
    }

    /* ==================== 调度 ==================== */

    /** 每分钟整点唤醒：命中配置小时且今日未跑才执行 */
    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        if (!enabled) return;
        String today = LocalDate.now().toString();
        int hour = LocalTime.now().getHour();
        for (String kind : KINDS) {
            int[] hs = hours.get(kind);
            if (hs == null) continue;
            boolean hourMatch = false;
            for (int h : hs) if (h == hour) hourMatch = true;
            if (!hourMatch) continue;
            if (today.equals(lastRunDate.get(kind))) continue;
            log.info("[WBAUTO] 定时执行 {}（{} 点档）", kind, hour);
            runJob(kind, today, true);
        }
    }

    /** 手动立即执行（不消耗当日档位） */
    public JobState runNow(String kind) {
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("未知任务类型：" + kind);
        return runJob(kind, LocalDate.now().toString(), false);
    }

    /** 一键执行全部任务（按 checkin→report→tasks→streak→travel→compensation 串行；不消耗当日档位） */
    public List<JobState> runAllNow() {
        List<JobState> out = new ArrayList<>();
        String today = LocalDate.now().toString();
        for (String kind : KINDS) out.add(runJob(kind, today, false));
        return out;
    }

    /* ==================== 执行 ==================== */

    private JobState runJob(String kind, String today, boolean scheduled) {
        synchronized (lock) {
            if (!running.compareAndSet(false, true)) {
                JobState s = states.get(kind);
                JobState busy = stateOf(kind);
                busy.setMessage("已有自动化任务在执行中，请稍后");
                return busy;
            }
            try {
                return doRun(kind, today, scheduled);
            } finally {
                running.set(false);
            }
        }
    }

    private JobState doRun(String kind, String today, boolean scheduled) {
        List<WbPoolAccount> accounts = eligibleAccounts();
        int ok = 0, failed = 0, claimed = 0;
        long credit = 0, energy = 0;
        List<String> detail = new ArrayList<>();
        String progress = null;
        int total = listAccounts().size();
        if (total > accounts.size()) {
            detail.add("⏭ 跳过 " + (total - accounts.size()) + " 个非正常状态账号（停用/判死/冷却/海外）");
        }
        for (int i = 0; i < accounts.size(); i++) {
            WbPoolAccount acc = accounts.get(i);
            try {
                switch (kind) {
                    case "checkin": {
                        long c = runCheckin(acc, detail);
                        credit += c;
                        record(acc.getId(), today, "checkinCredit", c);
                        break;
                    }
                    case "report": {
                        int days = runReport(acc);
                        detail.add(acc.getLabel() + "：连登 " + days + " 天");
                        break;
                    }
                    case "events": {
                        runEvents(acc, detail);
                        break;
                    }
                    case "tasks": {
                        long[] r = runTasks(acc, detail);
                        claimed += (int) r[0]; credit += r[1]; energy += r[2];
                        record(acc.getId(), today, "credit", r[1]);
                        record(acc.getId(), today, "energy", r[2]);
                        record(acc.getId(), today, "claimed", r[0]);
                        break;
                    }
                    case "streak": {
                        long c = runStreak(acc, detail);
                        credit += c;
                        record(acc.getId(), today, "bonusCredit", c);
                        break;
                    }
                    case "travel": {
                        long c = runTravel(acc, detail);
                        credit += c;
                        record(acc.getId(), today, "travelCredit", c);
                        break;
                    }
                    case "compensation": {
                        long c = runCompensation(acc, detail);
                        credit += c;
                        record(acc.getId(), today, "compensationCredit", c);
                        break;
                    }
                    default:
                        break;
                }
                ok++;
            } catch (Exception e) {
                failed++;
                detail.add("❌ " + acc.getLabel() + "：" + brief(e.getMessage()));
                log.warn("[WBAUTO] {} 账号 {} 失败：{}", kind, acc.getLabel(), e.getMessage());
            }
            if (i < accounts.size() - 1) {
                try { Thread.sleep(ACCOUNT_DELAY_MS); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
            }
        }

        JobState s = states.get(kind);
        if (scheduled) lastRunDate.put(kind, today);
        s.setLastRunDate(today);
        s.setLastRunAtMs(System.currentTimeMillis());
        s.setOk(ok);
        s.setFailed(failed);
        s.setCredit(credit);
        s.setEnergy(energy);
        s.setClaimed(claimed);
        StringBuilder msg = new StringBuilder(ok + " 个账号成功");
        if (failed > 0) msg.append("，").append(failed).append(" 失败");
        if ("tasks".equals(kind)) msg.append("，领取 ").append(claimed).append(" 个任务奖励（+").append(credit).append(" 积分）");
        else if (credit > 0) msg.append("，+").append(credit).append(" 积分");
        if (progress != null) msg.append("（").append(progress).append("）");
        s.setMessage(msg.toString());
        s.setDetail(detail);
        saveConfig();
        log.info("[WBAUTO] {} 完成：{}", kind, s.getMessage());
        return s;
    }

    /**
     * 参与自动化的账号：**仅限正常状态**——管理员启用、未判死、无冷却（含积分耗尽冷却），
     * 且 CN 域名（global 无成长体系）。非正常状态账号跳过：对冷却/判死号打成长接口
     * 只会收获 401/429，浪费配额且污染失败记录。
     */
    private List<WbPoolAccount> eligibleAccounts() {
        List<WbPoolAccount> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (WbPoolAccount a : accountPool.statusRows().isEmpty() ? Collections.<WbPoolAccount>emptyList() : listAccounts()) {
            if (a.getCredential().isGlobal()) continue; // global 无成长体系
            if (!a.isEnabled()) continue;                       // 管理员停用
            if (a.getCredentialDeadUntilMs() > now) continue;   // 会话判死
            if (a.getCooldownUntilMs() > now) continue;         // 积分耗尽冷却
            // xdpool ensureFresh：任务端点不吃网关的 acquireAccount 临期刷新，这里补齐——
            // 否则 token 过期后 tasks 全程 401「HTTP 401 非 JSON 响应」
            WbPoolCredential c = a.getCredential();
            boolean expiring = c.getExpiresAtMs() == null || c.getExpiresAtMs() <= 0
                    || c.getExpiresAtMs() < now + 60_000L;
            if (expiring && c.getRefreshToken() != null && !c.getRefreshToken().isEmpty()) {
                try {
                    client.refreshAccount(a.getId());
                    WbPoolAccount fresh = accountPool.get(a.getId());
                    if (fresh != null) a = fresh;
                } catch (Exception e) {
                    log.warn("[WBAUTO] {} 令牌刷新失败（继续用旧令牌尝试）：{}", a.getLabel(), e.getMessage());
                }
            }
            out.add(a);
        }
        return out;
    }

    /* ==================== 各任务 ==================== */

    private long runCheckin(WbPoolAccount acc, List<String> detail) throws Exception {
        try {
            JSONObject claim = growth.claimDailyCheckin(acc.getCredential());
            long c = claim.getLongValue("credit");
            if (c > 0) detail.add(acc.getLabel() + "：签到 +" + c);
            return c;
        } catch (WbUpstreamException e) {
            if (growth.isAlreadyCheckin(e)) {
                detail.add(acc.getLabel() + "：今日已签到");
                return 0;
            }
            throw e;
        }
    }

    /** 上报后回读连登天数作为落地判据（上游 200 也可能静默丢事件） */
    private int runReport(WbPoolAccount acc) throws Exception {
        growth.reportActivity(acc.getCredential());
        return growth.growthStreakDays(acc.getCredential());
    }

    /** 进度读取：优先顶层 target/current，缺失回退嵌套 progress.{current,target}（xdpool parseTask） */
    private static long[] progressOf(JSONObject t) {
        long target = t.getLongValue("target"), current = t.getLongValue("current");
        if (target == 0 && current == 0) {
            JSONObject p = t.getJSONObject("progress");
            if (p != null) {
                current = p.getLongValue("current");
                target = p.getLongValue("target");
            }
        }
        return new long[]{current, target};
    }

    /** 报名未报名且未锁定的任务 → 重读 → 领取已达标的奖励；返回 [claimed, credit, energy] */
    private long[] runTasks(WbPoolAccount acc, List<String> detail) throws Exception {
        WbPoolCredential c = acc.getCredential();
        JSONArray tasks = growth.listTasks(c);
        List<String> open = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            JSONObject t = tasks.getJSONObject(i);
            if (t == null) continue;
            if ("not_accepted".equals(t.getString("accept_status")) && !t.getBooleanValue("locked")) {
                open.add(t.getString("task_code"));
            }
        }
        growth.acceptTasks(c, open);

        JSONArray refreshed = growth.listTasks(c);
        long claimed = 0, credit = 0, energy = 0;
        for (int i = 0; i < refreshed.size(); i++) {
            JSONObject t = refreshed.getJSONObject(i);
            if (t == null) continue;
            boolean alreadyClaimed = "claimed".equals(t.getString("accept_status"));
            long[] prog = progressOf(t);
            long target = prog[1], current = prog[0];
            boolean claimable = !alreadyClaimed && target > 0 && current >= target && !t.getBooleanValue("locked");
            if (!claimable) continue;
            JSONObject reward = growth.claimTaskReward(c, t.getString("task_code"));
            long rc = reward.getLongValue("credit"), re = reward.getLongValue("energy");
            if (rc > 0 || re > 0) {
                claimed++;
                credit += rc; energy += re;
                detail.add(acc.getLabel() + "：领奖「" + t.getString("title") + "」 +" + rc + "c +" + re + "e");
            }
            Thread.sleep(CLAIM_DELAY_MS);
        }
        return new long[]{claimed, credit, energy};
    }

    /**
     * 事件链补分（xdpool sendEventChains 移植）：对仍未达标且未锁定的客户端评分任务，
     * 构造并上报对应事件链。之后 tasks 批次的领奖即可当场收数（评分几秒内落地）。
     * 每条链失败只影响自己，不中断整轮。
     */
    private void runEvents(WbPoolAccount acc, List<String> detail) throws Exception {
        WbPoolCredential c = acc.getCredential();
        JSONArray tasks = growth.listTasks(c);
        boolean sentAnything = false;
        for (int i = 0; i < tasks.size(); i++) {
            JSONObject t = tasks.getJSONObject(i);
            if (t == null) continue;
            String code = t.getString("task_code");
            long[] prog = progressOf(t);
            long target = prog[1], current = prog[0];
            // 诊断：所有含「专家」的任务打决策日志（任务码/进度/锁定/jump_url 形态），
            // 用于排查新定向专家任务为何未发链
            String title = t.getString("title");
            if (title != null && title.contains("专家")) {
                log.info("[WBAUTO] events {} 专家任务 title={} code={} locked={} prog={}/{} jumpUrl={}",
                        acc.getLabel(), title, code, t.getBooleanValue("locked"), current, target, t.getString("jump_url"));
            }
            // 定向专家类：任务码不可枚举（上游按专家随意命名，如电商运营/抖音策略师/海报设计师），
            // 以 jump_url 的 expertId deeplink 为准走定向链；其余事件链任务仍走码白名单
            if (!EVENT_CHAIN_CODES.contains(code) && expertIdFromJumpUrl(t) == null) continue;
            if (t.getBooleanValue("locked") || (target > 0 && current >= target)) continue;
            try {
                int sent = sendChainsFor(acc, c, code, t, detail);
                if (sent > 0) sentAnything = true;
            } catch (Exception e) {
                detail.add("⚠ " + acc.getLabel() + "：事件链 " + code + " 失败 " + brief(e.getMessage()));
                log.warn("[WBAUTO] events {} {} 失败：{}", acc.getLabel(), code, e.getMessage());
            }
        }
        // 等评分落地，同轮随后的 tasks 批次即可领
        if (sentAnything) {
            detail.add(acc.getLabel() + "：事件链已发送，等待评分落地");
            Thread.sleep(WbTaskEventChains.EVENT_SCORE_WAIT_MS);
        }
    }

    /** 按任务码构造并发送链，返回发送条数 */
    private int sendChainsFor(WbPoolAccount acc, WbPoolCredential c, String code, JSONObject task,
                              List<String> detail) throws Exception {
        // 「体验某专家」类（WorkBuddy_Valorant / expert_use_*）：专家 id 编码在 jump_url
        // deeplink（expertId=ex_XXX）而非市场列表，须按任务行定向召唤
        String jumpExpertId = expertIdFromJumpUrl(task);
        if (jumpExpertId != null) {
            String name = expertNameFromTitle(task);
            return sendFixedExpertChain(acc, c, code, jumpExpertId, name, detail);
        }
        List<WbTaskEventChains.Chain> chains;
        switch (code) {
            case "Buddy_App":
            case "Buddy_App_QQ":
                chains = Collections.singletonList(WbTaskEventChains.buddyAppChain());
                break;
            case "create_canvas":
                chains = Collections.singletonList(WbTaskEventChains.canvasChain());
                break;
            case "automation_1":
                chains = Collections.singletonList(WbTaskEventChains.automationChain());
                break;
            case "RichMeow_Chat":
                chains = Collections.singletonList(WbTaskEventChains.chatChain());
                break;
            case "playbook_prompt":
                chains = Collections.singletonList(WbTaskEventChains.playbookChain());
                break;
            case "template_5":
                chains = WbTaskEventChains.templateChains();
                break;
            case "Hp_Appearance":
                // 真实客户端行为是先 PATCH 皮肤再在设置关闭时报事件，只有事件不够
                growth.setAppearanceTheme(c, WbTaskEventChains.APPEARANCE_THEME_KEY);
                Thread.sleep(2_000);
                chains = Collections.singletonList(WbTaskEventChains.appearanceChain());
                break;
            case "Library_read":
                chains = Collections.singletonList(WbTaskEventChains.libraryReadChain());
                break;
            case "skill_1": {
                JSONObject chat = growth.openConversation(c, null);
                if (chat == null) throw new IllegalStateException("无法打开真实会话");
                chains = Collections.singletonList(
                        WbTaskEventChains.skillChain(chat.getString("conversationId"), chat.getString("requestId")));
                break;
            }
            case "expert_5":
                return sendExpertChains(acc, c, "agent", task, detail);
            case "Expert_team_use_3":
                return sendExpertChains(acc, c, "team", task, detail);
            case "Expert_lighthouse":
                return sendLighthouseChain(acc, c, detail);
            default:
                return 0;
        }
        int sent = 0;
        for (WbTaskEventChains.Chain chain : chains) {
            sendChain(c, chain);
            sent++;
            Thread.sleep(500);
        }
        if (sent > 0) detail.add(acc.getLabel() + "：" + code + " 已发 " + sent + " 条链（" + task.getString("title") + "）");
        return sent;
    }

    /** expert 家族：召唤事件 + 真实会话（X-Expert-Id）+ 使用事件；只补缺口，链间留间隔防合并计分 */
    private int sendExpertChains(WbPoolAccount acc, WbPoolCredential c, String expertType,
                                 JSONObject task, List<String> detail) throws Exception {
        long[] prog = progressOf(task);
        int needed = (int) Math.max(0, prog[1] - prog[0]);
        if (needed == 0) return 0;
        JSONArray experts = growth.marketExpertList(c, expertType);
        int sent = 0;
        for (int i = 0; i < experts.size() && sent < needed; i++) {
            JSONObject expert = experts.getJSONObject(i);
            try {
                growth.reportDesktopEvents(c, WbTaskEventChains.expertSummonEvents(expert));
                Thread.sleep(500);
                JSONObject chat = growth.openConversation(c, expert.getString("expertId"));
                if (chat == null) continue;
                String convId = chat.getString("conversationId");
                String reqId = chat.getString("requestId");
                JSONArray events = WbTaskEventChains.expertChatEvents(expert, convId, reqId);
                events.add(WbTaskEventChains.expertActualUseEvent(expert, convId, reqId, "craft"));
                growth.reportDesktopEvents(c, events);
                sent++;
                detail.add(acc.getLabel() + "：expert 使用 " + expert.getString("displayName") + "（" + sent + "/" + needed + "）");
            } catch (Exception e) {
                log.warn("[WBAUTO] events {} expert {} 失败：{}", acc.getLabel(), expert.getString("expertId"), e.getMessage());
            }
            Thread.sleep(WbTaskEventChains.EXPERT_GAP_MS);
        }
        return sent;
    }

    /** Expert_lighthouse：同 expert 流程，但 use 事件要对齐 mode=LOCAL、type 空、cost=0 */
    private int sendLighthouseChain(WbPoolAccount acc, WbPoolCredential c, List<String> detail) throws Exception {
        JSONObject expert = null;
        JSONArray listed = growth.marketExpertList(c, "agent");
        for (int i = 0; i < listed.size(); i++) {
            if (WbTaskEventChains.LIGHTHOUSE_EXPERT_ID.equals(listed.getJSONObject(i).getString("expertId"))) {
                expert = listed.getJSONObject(i);
                break;
            }
        }
        if (expert == null) {
            expert = new JSONObject();
            expert.put("expertId", WbTaskEventChains.LIGHTHOUSE_EXPERT_ID);
            expert.put("expertType", "agent");
            expert.put("displayName", WbTaskEventChains.LIGHTHOUSE_EXPERT_NAME);
            expert.put("profession", "");
            expert.put("version", "1.0.0");
            expert.put("categories", new JSONArray());
        }
        growth.reportDesktopEvents(c, WbTaskEventChains.expertSummonEvents(expert));
        Thread.sleep(500);
        JSONObject chat = growth.openConversation(c, expert.getString("expertId"));
        if (chat == null) throw new IllegalStateException("Expert_lighthouse 无法打开真实会话");
        String convId = chat.getString("conversationId");
        String reqId = chat.getString("requestId");
        JSONObject use = WbTaskEventChains.expertActualUseEvent(expert, convId, reqId, "LOCAL");
        use.put("type", "");
        use.put("cost", 0);
        JSONArray events = WbTaskEventChains.expertChatEvents(expert, convId, reqId);
        events.add(use);
        growth.reportDesktopEvents(c, events);
        detail.add(acc.getLabel() + "：Expert_lighthouse 链已发送");
        return 1;
    }

    /** 一条链按其通道发送（通道即指纹族，发错通道=收下不计分） */
    private void sendChain(WbPoolCredential c, WbTaskEventChains.Chain chain) throws Exception {
        if (chain.web) {
            growth.reportWebEvent(c, chain.eventCode, chain.pageUrl, chain.elementId, chain.elementName);
        } else {
            growth.reportDesktopEvents(c, chain.events);
        }
    }

    /** 事件链任务码全集（xdpool EVENT_CHAIN_BUILDERS + 2026-10 新增体验专家类） */
    private static final Set<String> EVENT_CHAIN_CODES = new LinkedHashSet<>(Arrays.asList(
            "Buddy_App", "Buddy_App_QQ", "create_canvas", "automation_1", "RichMeow_Chat",
            "playbook_prompt", "template_5", "Hp_Appearance", "Library_read", "skill_1",
            "expert_5", "Expert_team_use_3", "Expert_lighthouse",
            "WorkBuddy_Valorant", "expert_use_maidehao", "expert_use_douyinyin",
            "expert_use_shangjia_xuanchuan", "expert_use_bowangwang"));

    /** 从任务行 jump_url 提取固定专家 id（deeplink expertId=ex_XXX）；无则 null。
     *  jump_url 里的 deeplink 可能整体 URL 编码（expertId%3Dex_XXX），先解码再匹配。 */
    private static String expertIdFromJumpUrl(JSONObject task) {
        String url = task.getString("jump_url");
        if (url == null || url.isEmpty()) return null;
        try {
            if (url.contains("%")) url = java.net.URLDecoder.decode(url, "UTF-8");
        } catch (Exception ignore) { }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("expertId=(ex_[A-Za-z0-9]+)").matcher(url);
        return m.find() ? m.group(1) : null;
    }

    /** 从任务标题「体验「XXX」」里取专家名；取不到就用任务码 */
    private static String expertNameFromTitle(JSONObject task) {
        String title = task.getString("title");
        if (title == null || title.isEmpty()) return task.getString("task_code");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("「([^」]+)」").matcher(title);
        return m.find() ? m.group(1) : title;
    }

    /** 定向专家链：召唤 → 真实会话（X-Expert-Id）→ 使用事件（新增体验专家类专用） */
    private int sendFixedExpertChain(WbPoolAccount acc, WbPoolCredential c, String code,
                                     String expertId, String expertName, List<String> detail) throws Exception {
        JSONObject expert = new JSONObject();
        expert.put("expertId", expertId);
        expert.put("expertType", "agent");
        expert.put("displayName", expertName);
        expert.put("profession", "");
        expert.put("version", "1.0.0");
        expert.put("categories", new JSONArray());
        growth.reportDesktopEvents(c, WbTaskEventChains.expertSummonEvents(expert));
        Thread.sleep(500);
        JSONObject chat = growth.openConversation(c, expertId);
        if (chat == null) throw new IllegalStateException("无法打开真实会话（" + expertName + "）");
        String convId = chat.getString("conversationId");
        String reqId = chat.getString("requestId");
        JSONArray events = WbTaskEventChains.expertChatEvents(expert, convId, reqId);
        events.add(WbTaskEventChains.expertActualUseEvent(expert, convId, reqId, "craft"));
        growth.reportDesktopEvents(c, events);
        detail.add(acc.getLabel() + "：" + code + " 定向链已发（" + expertName + "）");
        return 1;
    }

    /** 连登兑换 + 抽奖；返回本次兑换的积分 */
    private long runStreak(WbPoolAccount acc, List<String> detail) throws Exception {
        WbPoolCredential c = acc.getCredential();
        JSONObject data = growth.growthStreakFull(c);
        JSONObject streak = data.getJSONObject("streak") == null ? new JSONObject() : data.getJSONObject("streak");
        JSONObject redemption = data.getJSONObject("redemption_status") == null ? new JSONObject() : data.getJSONObject("redemption_status");
        long days = streak.getLongValue("days");
        JSONArray tiers = redemption.getJSONArray("tiers");
        long credit = 0;
        String pending = null;
        if (tiers != null) {
            for (int i = 0; i < tiers.size(); i++) {
                JSONObject t = tiers.getJSONObject(i);
                if (t == null) continue;
                String tier = t.getString("tier");
                String status = redemption.getString("tier_" + tier + "_status");
                if ("locked".equals(status) || "claimed".equals(status)) {
                    if ("locked".equals(status) && pending == null) {
                        long need = t.getLongValue("days") - days;
                        pending = tier + " 还差 " + Math.max(0, need) + " 天";
                    }
                    continue;
                }
                try {
                    growth.redeemStreakTier(c, tier);
                    long tc = t.getLongValue("credit");
                    credit += tc;
                    detail.add(acc.getLabel() + "：连登兑换 " + tier + " +" + tc + "c");
                } catch (Exception e) {
                    log.warn("[WBAUTO] streak 兑换 {} {} 失败：{}", acc.getLabel(), tier, e.getMessage());
                }
                Thread.sleep(CLAIM_DELAY_MS);
            }
        }
        int chances = growth.lotteryChances(c);
        for (int d = 0; d < chances; d++) {
            try {
                growth.lotteryDraw(c);
            } catch (Exception e) {
                log.warn("[WBAUTO] lottery {} 抽奖失败：{}", acc.getLabel(), e.getMessage());
                break;
            }
            Thread.sleep(CLAIM_DELAY_MS);
        }
        if (pending != null) detail.add(acc.getLabel() + "：" + pending);
        return credit;
    }

    /** 猫猫旅行：一趟推进一步（到达领奖 / 闲着出发 / 领养） */
    private long runTravel(WbPoolAccount acc, List<String> detail) throws Exception {
        WbPoolCredential c = acc.getCredential();
        JSONObject buddy = growth.buddyInfo(c);
        if (buddy == null) {
            try {
                growth.buddyAgree(c);
                growth.buddyAdoptFirst(c);
                detail.add(acc.getLabel() + "：首次领养猫猫");
            } catch (Exception e) {
                detail.add(acc.getLabel() + "：猫猫领养条件未满足（需当日活跃）");
            }
            return 0;
        }
        JSONObject travel = growth.buddyTravelStatus(c);
        String state = travel.getString("state");
        if ("arrived".equals(state)) {
            long recordId = travel.getLongValue("record_id");
            if (recordId <= 0) return 0;
            long reward = growth.buddyTravelClaim(c, recordId);
            if (reward > 0) detail.add(acc.getLabel() + "：旅行归来 +" + reward + "c");
            return reward;
        }
        if ("idle".equals(state) && !travel.getBooleanValue("daily_limit_reached")) {
            growth.buddyTravelDepart(c);
            detail.add(acc.getLabel() + "：猫猫已出发（下轮领取）");
            return 0;
        }
        return 0;
    }

    /** 故障补偿：读-领两段式；无补偿是常态空态 */
    private long runCompensation(WbPoolAccount acc, List<String> detail) throws Exception {
        WbPoolCredential c = acc.getCredential();
        JSONObject status = growth.compensationStatus(c);
        boolean active = status.getBooleanValue("active");
        boolean claimed = status.getBooleanValue("claimed");
        long creditNum = status.getLongValue("credit_num");
        if (!active || claimed || creditNum <= 0) return 0;
        JSONObject claim = growth.claimCompensation(c);
        long got = claim.getLongValue("credit_num") > 0 ? claim.getLongValue("credit_num") : creditNum;
        if (got > 0) detail.add(acc.getLabel() + "：故障补偿 +" + got);
        return got;
    }

    /* ==================== 视图/配置 ==================== */

    private List<WbPoolAccount> listAccounts() {
        List<WbPoolAccount> out = new ArrayList<>();
        for (Map<String, Object> row : accountPool.statusRows()) {
            WbPoolAccount a = accountPool.get(String.valueOf(row.get("id")));
            if (a != null) out.add(a);
        }
        return out;
    }

    private JobState stateOf(String kind) {
        JobState s = new JobState();
        s.setKind(kind);
        return s;
    }

    private void record(String accountId, String date, String field, long v) {
        if (v <= 0) return;
        earnings.computeIfAbsent(accountId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(date, k -> new ConcurrentHashMap<>())
                .merge(field, v, Long::sum);
    }

    public Map<String, Object> statusView() {
        synchronized (lock) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enabled", enabled);
            m.put("hours", hours);
            List<Map<String, Object>> jobs = new ArrayList<>();
            for (String k : KINDS) {
                JobState s = states.get(k);
                Map<String, Object> j = new LinkedHashMap<>();
                j.put("kind", k);
                j.put("hours", hours.get(k));
                j.put("lastRunDate", s.getLastRunDate());
                j.put("lastRunAtMs", s.getLastRunAtMs());
                j.put("ok", s.getOk());
                j.put("failed", s.getFailed());
                j.put("credit", s.getCredit());
                j.put("claimed", s.getClaimed());
                j.put("message", s.getMessage());
                j.put("detail", s.getDetail());
                jobs.add(j);
            }
            m.put("jobs", jobs);
            return m;
        }
    }

    public void setEnabled(boolean on) {
        synchronized (lock) {
            enabled = on;
            saveConfig();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /* ---------------- automation.json 持久化 ---------------- */

    private Path configPath() {
        return Paths.get(System.getProperty("user.dir"), "data", "wb-pool", "automation.json");
    }

    private void loadConfig() {
        try {
            JSONObject doc = JSON.parseObject(new String(Files.readAllBytes(configPath()), StandardCharsets.UTF_8));
            if (doc == null) return;
            Boolean en = doc.getBoolean("enabled");
            enabled = en == null ? true : en;
            JSONObject h = doc.getJSONObject("hours");
            if (h != null) {
                for (String k : KINDS) {
                    JSONArray arr = h.getJSONArray(k);
                    if (arr != null && !arr.isEmpty()) {
                        int[] xs = new int[arr.size()];
                        for (int i = 0; i < arr.size(); i++) xs[i] = arr.getIntValue(i);
                        hours.put(k, xs);
                    }
                }
            }
            JSONObject lrd = doc.getJSONObject("lastRunDate");
            if (lrd != null) for (String k : KINDS) lastRunDate.put(k, lrd.getString(k) == null ? "" : lrd.getString(k));
            JSONObject ea = doc.getJSONObject("earnings");
            if (ea != null) {
                for (String acc : ea.keySet()) {
                    JSONObject dates = ea.getJSONObject(acc);
                    if (dates == null) continue;
                    for (String date : dates.keySet()) {
                        JSONObject fields = dates.getJSONObject(date);
                        if (fields == null) continue;
                        for (String f : fields.keySet()) record(acc, date, f, fields.getLongValue(f));
                    }
                }
            }
        } catch (Exception ignore) {
            // 首次运行无配置文件：用默认值
        }
    }

    private void saveConfig() {
        try {
            Path p = configPath();
            Files.createDirectories(p.getParent());
            JSONObject doc = new JSONObject();
            doc.put("version", 1);
            doc.put("enabled", enabled);
            JSONObject h = new JSONObject();
            for (String k : KINDS) {
                JSONArray arr = new JSONArray();
                for (int x : hours.get(k)) arr.add(x);
                h.put(k, arr);
            }
            doc.put("hours", h);
            JSONObject lrd = new JSONObject();
            for (String k : KINDS) lrd.put(k, lastRunDate.get(k));
            doc.put("lastRunDate", lrd);
            JSONObject ea = new JSONObject();
            for (Map.Entry<String, Map<String, Map<String, Long>>> e1 : earnings.entrySet()) {
                JSONObject dates = new JSONObject();
                for (Map.Entry<String, Map<String, Long>> e2 : e1.getValue().entrySet()) {
                    JSONObject fields = new JSONObject();
                    fields.putAll(e2.getValue());
                    dates.put(e2.getKey(), fields);
                }
                ea.put(e1.getKey(), dates);
            }
            doc.put("earnings", ea);
            Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
            Files.write(tmp, doc.toJSONString().getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log.warn("[WBAUTO] 配置写入失败（不影响运行）：{}", e.getMessage());
        }
    }

    private static String brief(String s) {
        return s == null ? "" : (s.length() > 120 ? s.substring(0, 120) : s);
    }
}