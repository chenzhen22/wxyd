package com.cyz.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.mapper.mysqlMapper.DayInfoMapper;
import com.cyz.pojo.DayInfo;
import com.cyz.pojo.DayInfoSubscribe;
import com.cyz.push.PushService;
import com.cyz.util.SMUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 金融日报：外部 SM2 加密 + SM4 加签推送（/common/dayinfo.do，免登录靠验签+解密鉴权），
 * 按类型每日一条（同日覆盖更新），登录用户可查看与订阅提醒。
 * <p>
 * 推送协议（三步串行校验，任一失败即拒绝）：
 * <ol>
 *   <li>时间戳 ±5 分钟窗口（防重放，沿用 tbphx.do 思路）；</li>
 *   <li>SM4 加签验证：sign = SM4-ECB(SM3(data密文hex + timestamp))，解出摘要比对；</li>
 *   <li>SM2 解密密文，得到 {"type":"finance","data":{"date","title","md"}}。</li>
 * </ol>
 * SM4 为对称加密没有标准签名，此处用「摘要的对称加密」作为加签手段。
 */
@Service
@Slf4j
public class DayInfoService {

    /** 请求时间戳允许窗口（毫秒）：±5 分钟 */
    private static final long TIMESTAMP_WINDOW_MS = 5 * 60 * 1000L;

    /** 默认日报类型：type 为空视为金融日报 */
    private static final String TYPE_FINANCE = "finance";

    /** 推送里「今日要点」一行摘要的截断长度 */
    private static final int TEASER_LEN = 100;

    @Resource
    private DayInfoMapper dayInfoMapper;

    @Resource
    private PushService pushService;

    @Value("${wxyd.dayinfo.sm2-private-key:}")
    private String sm2PrivateKey;

    @Value("${wxyd.dayinfo.sm4-key:}")
    private String sm4Key;

    /** 分享链接基地址（如 https://域名/api），留空则推送消息不带链接 */
    @Value("${wxyd.dayinfo.base-url:}")
    private String baseUrl;

    /** 订阅提醒发送时间窗口起始小时（含），默认 8 点 */
    @Value("${wxyd.dayinfo.notify-window-start:8}")
    private int notifyWindowStart;

    /** 订阅提醒发送时间窗口结束小时（不含），默认 22 点；窗口外入库的日报推迟到窗口内再推 */
    @Value("${wxyd.dayinfo.notify-window-end:22}")
    private int notifyWindowEnd;

    /** 订阅提醒旁路发送（单线程串行，避免阻塞定时任务线程；失败仅记日志） */
    private final ExecutorService notifyExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dayinfo-notify");
        t.setDaemon(true);
        return t;
    });

    /* ==================== 推送（免登录，验签+解密） ==================== */

    /**
     * 接收加密推送并入库。
     *
     * @param timestamp 毫秒时间戳（参与签名）
     * @param dataHex   SM2 密文 hex（明文 = {"type":"..","data":{"date","title","md"}}）
     * @param signHex   SM4-ECB(SM3(dataHex + timestamp)) 的 hex
     * @return 错误信息；null=成功
     */
    public String push(Long timestamp, String dataHex, String signHex) {
        if (sm2PrivateKey == null || sm2PrivateKey.trim().isEmpty()
                || sm4Key == null || sm4Key.trim().isEmpty()) {
            return "日报推送未配置（wxyd.dayinfo.sm2-private-key / wxyd.dayinfo.sm4-key）";
        }
        if (timestamp == null || dataHex == null || dataHex.trim().isEmpty() || signHex == null || signHex.trim().isEmpty()) {
            return "参数缺失（timestamp/data/sign）";
        }
        // 1. 时间戳窗口
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > TIMESTAMP_WINDOW_MS) {
            log.warn("[DAYINFO] 推送拒绝：时间戳超窗，ts={} now={}", timestamp, now);
            return "时间戳超出允许窗口";
        }
        // 2. SM4 加签验证（对密文验签：完整性优先，验签通过才动私钥）
        String expect = SMUtil.sm3(dataHex.trim() + timestamp);
        String actual;
        try {
            actual = SMUtil.sm4EcbDecrypt(sm4Key.trim(), signHex.trim());
        } catch (Exception e) {
            log.warn("[DAYINFO] 推送拒绝：sign 解密失败");
            return "验签失败";
        }
        if (!constantTimeEquals(expect, actual)) {
            log.warn("[DAYINFO] 推送拒绝：摘要不匹配");
            return "验签失败";
        }
        // 3. SM2 解密
        String plain;
        try {
            plain = SMUtil.sm2Decrypt(sm2PrivateKey.trim(), dataHex.trim());
        } catch (Exception e) {
            log.warn("[DAYINFO] 推送拒绝：SM2 解密失败：{}", e.getMessage());
            return "解密失败";
        }
        JSONObject obj;
        try {
            obj = JSON.parseObject(plain);
        } catch (Exception e) {
            return "解密后不是合法 JSON";
        }
        if (obj == null || obj.getJSONObject("data") == null) {
            return "数据结构非法（缺少 data）";
        }
        String type = normalizeType(obj.getString("type"));
        JSONObject data = obj.getJSONObject("data");
        String md = data.getString("md");
        if (md == null || md.trim().isEmpty()) {
            return "data.md 不能为空";
        }
        String infoDate = parseDate(data.getString("date"));
        String title = data.getString("title");
        if (title == null || title.trim().isEmpty()) {
            title = (TYPE_FINANCE.equals(type) ? "金融日报 " : type + "日报 ") + infoDate;
        }
        title = title.trim();
        // 简洁版：推送方可传（订阅推送优先用），缺省 null 由后端自动截取正文
        String brief = data.getString("brief");

        // 按 (type, info_date) upsert：同日重复推送覆盖，只留最新
        DayInfo exist = dayInfoMapper.getByTypeAndDate(type, infoDate);
        DayInfo info = new DayInfo();
        info.setType(type);
        info.setInfoDate(infoDate);
        info.setTitle(title);
        info.setContent(md);
        info.setBrief(brief);
        boolean isNew;
        if (exist == null) {
            dayInfoMapper.insert(info); // notified=0，由 notifyJob 在时间窗口内发送订阅提醒
            isNew = true;
        } else {
            info.setId(exist.getId());
            dayInfoMapper.updateContent(info);
            isNew = false;
        }
        log.info("[DAYINFO] 日报入库：type={} date={} title={} mode={}", type, infoDate, title, isNew ? "insert" : "update");
        return null;
    }

    /** type 归一化：空 = 金融日报；仅保留字母数字下划线，超长截断（存库列 32 字符） */
    private static String normalizeType(String type) {
        String t = type == null ? "" : type.trim();
        if (t.isEmpty() || "金融日报".equals(t)) {
            return TYPE_FINANCE;
        }
        t = t.replaceAll("[^a-zA-Z0-9_-]", "");
        return t.isEmpty() ? TYPE_FINANCE : t.substring(0, Math.min(t.length(), 32));
    }

    /** 日期解析：缺省取当天；非法格式拒绝 */
    private static String parseDate(String date) {
        String d = date == null || date.trim().isEmpty() ? LocalDate.now().toString() : date.trim();
        try {
            return LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("data.date 非法（须为 YYYY-MM-DD）");
        }
    }

    /** 恒时比较，避免逐字符短路泄露比对进度 */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    /* ==================== 查询（登录用户） ==================== */

    public List<DayInfo> list(String type, String keyword, String month, int pageNum, int pageSize) {
        String t = normalizeType(type);
        int size = Math.min(Math.max(pageSize, 1), 50);
        int page = Math.max(pageNum, 1);
        return dayInfoMapper.listByType(t, emptyToNull(keyword), month == null ? null : month.trim(),
                (page - 1) * size, size);
    }

    public int count(String type, String keyword, String month) {
        return dayInfoMapper.countByType(normalizeType(type), emptyToNull(keyword), month == null ? null : month.trim());
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    public DayInfo detail(Long id) {
        return id == null ? null : dayInfoMapper.getById(id);
    }

    public DayInfo latest(String type) {
        return dayInfoMapper.getLatest(normalizeType(type));
    }

    /* ==================== 订阅（登录用户，一人多条） ==================== */

    public static final int MAX_SUBSCRIBE_PER_USER = 10;

    /** 我的订阅列表（多个钉钉机器人 + 多个邮箱） */
    public List<DayInfoSubscribe> mySubscribe(long userId) {
        return dayInfoMapper.listByUser(userId);
    }

    /** 新增订阅；校验规则与羊毛雷达一致；robotId 归属在推送时由 PushService 校验 */
    public DayInfoSubscribe addSubscribe(long userId, String channel, Long robotId, String email) {
        String ch = channel == null ? "" : channel.trim();
        if (!"dingtalk".equals(ch) && !"email".equals(ch)) {
            throw new IllegalArgumentException("channel 非法（dingtalk | email）");
        }
        DayInfoSubscribe s = new DayInfoSubscribe();
        s.setUserId(userId);
        s.setChannel(ch);
        if ("dingtalk".equals(ch)) {
            if (robotId == null) {
                throw new IllegalArgumentException("钉钉通道必须选择机器人");
            }
            s.setRobotId(robotId);
        } else {
            if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException("邮件通道必须填写有效邮箱");
            }
            s.setEmail(email.trim());
        }
        if (dayInfoMapper.countByUser(userId) >= MAX_SUBSCRIBE_PER_USER) {
            throw new IllegalStateException("订阅数已达上限（" + MAX_SUBSCRIBE_PER_USER + " 条）");
        }
        s.setEnabled(1);
        dayInfoMapper.insertSubscribe(s);
        return dayInfoMapper.getSubById(s.getId());
    }

    public boolean deleteSubscribe(long userId, Long id) {
        return id != null && dayInfoMapper.deleteSubscribe(id, userId) > 0;
    }

    public boolean toggleSubscribe(long userId, Long id, boolean enabled) {
        return id != null && dayInfoMapper.updateSubscribeEnabled(id, userId, enabled ? 1 : 0) > 0;
    }

    /** 立即测试推送：把最新一期日报推给指定订阅（不看入库时间），用于配置后自检通道连通性 */
    public String testSubscribe(long userId, Long id) {
        DayInfoSubscribe s = id == null ? null : dayInfoMapper.getSubById(id);
        if (s == null || s.getUserId() != userId) {
            throw new IllegalArgumentException("订阅不存在");
        }
        DayInfo latest = dayInfoMapper.getLatest(TYPE_FINANCE);
        if (latest == null) {
            return "暂无日报可测试";
        }
        return pushOne(s, latest);
    }

    /* ==================== 订阅提醒（时间窗口定时任务） ==================== */

    /**
     * 订阅提醒定时任务：每 10 分钟扫描首次入库（notified=0）的日报，
     * 处于时间窗口（默认 8:00-22:00，wxyd.dayinfo.notify-window-start/end）内才发送；
     * 只推送当日（info_date=今天）的日报——深夜入库顺延到次日窗口的日报已非当日，不再推送，
     * 直接标记 notified=1 防止反复扫描；发送后置 notified=1，同日覆盖更新不重置（不重复提醒）。
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void notifyJob() {
        int hour = LocalTime.now().getHour();
        if (hour < notifyWindowStart || hour >= notifyWindowEnd) {
            return;
        }
        List<DayInfo> pending = dayInfoMapper.listUnnotified();
        if (pending == null || pending.isEmpty()) {
            return;
        }
        String today = LocalDate.now().toString();
        List<DayInfoSubscribe> subs = dayInfoMapper.listEnabledSubscribe();
        for (DayInfo info : pending) {
            if (info.getInfoDate() == null || !today.equals(info.getInfoDate())) {
                // 非当日日报不推送（含跨日顺延的），标记已处理避免每次扫描重复出现
                dayInfoMapper.markNotified(info.getId());
                log.info("[DAYINFO] 非当日日报跳过推送：info={} date={}", info.getId(), info.getInfoDate());
                continue;
            }
            if (subs != null && !subs.isEmpty()) {
                for (DayInfoSubscribe s : subs) {
                    notifyExecutor.execute(() -> {
                        try {
                            String err = pushOne(s, info);
                            if (err != null) {
                                log.warn("[DAYINFO] 订阅提醒失败：sub={} info={} err={}", s.getId(), info.getId(), err);
                            }
                        } catch (Exception e) {
                            log.error("[DAYINFO] 订阅提醒异常：sub={} info={}", s.getId(), info.getId(), e);
                        }
                    });
                }
            }
            dayInfoMapper.markNotified(info.getId());
            log.info("[DAYINFO] 订阅提醒已派发：info={} subs={}", info.getId(), subs == null ? 0 : subs.size());
        }
    }

    /** 组装并推送一条订阅；@return null=成功，非 null=失败原因。简洁版：链接放最前，引导注册 */
    private String pushOne(DayInfoSubscribe s, DayInfo info) {
        String link = shareLink(info);
        // 优先取推送方提供的简洁版（brief），完整展示不截断；
        // brief 缺省才由后端自动截取正文前 TEASER_LEN 字
        String brief = info.getBrief() != null && !info.getBrief().trim().isEmpty() ? info.getBrief().trim() : null;
        String teaser;
        if (brief != null) {
            teaser = mdToPlainText(brief);
        } else {
            teaser = mdToPlainText(info.getContent());
            if (teaser.length() > TEASER_LEN) {
                teaser = teaser.substring(0, TEASER_LEN) + "…";
            }
        }
        if ("dingtalk".equals(s.getChannel())) {
            StringBuilder md = new StringBuilder();
            md.append("### 金融日报 ").append(info.getInfoDate()).append(" 已更新\n\n");
            if (link != null) {
                md.append("🔗 [点击查看完整日报](").append(link).append(")\n\n");
            }
            md.append("> ").append(teaser).append('\n');
            if (link != null) {
                md.append("\n\n📡 [注册小泽助手账号](").append(registerLink())
                        .append(")，新日报自动推送到你的钉钉/邮箱");
            }
            return pushService.pushDingtalk(s.getUserId(), s.getRobotId(), "金融日报：" + info.getInfoDate(), md.toString());
        }
        // 邮件走 HTML：<a> 承载链接，不裸露 URL
        StringBuilder mail = new StringBuilder();
        mail.append("<p><b>金融日报 ").append(htmlEscape(info.getInfoDate())).append(" 已更新</b></p>");
        if (link != null) {
            mail.append("<p>🔗 <a href=\"").append(link).append("\">点击查看完整日报</a></p>");
        }
        mail.append("<p style=\"color:#666;\">").append(htmlEscape(teaser).replace("\n", "<br>")).append("</p>");
        if (link != null) {
            mail.append("<hr><p>📡 <a href=\"").append(registerLink())
                    .append("\">注册小泽助手账号</a>，新日报自动推送到你的钉钉/邮箱</p>");
        }
        return pushService.pushEmailHtml(s.getEmail(), "金融日报（" + info.getInfoDate() + "）", mail.toString());
    }

    /** HTML 转义（邮件正文片段用） */
    private static String htmlEscape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** 日报分享链接（免登录公开页 dayinfo-share.html，带访问令牌防 id 枚举）；base-url 未配置时返回 null */
    private String shareLink(DayInfo info) {
        if (info.getId() == null || baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }
        String base = baseUrl.trim();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
                + "/dayinfo-share.html?id=" + info.getId() + "&t=" + shareToken(info.getId());
    }

    /** 公开详情访问令牌：SM3(sm4Key + ":dayinfo:" + id) 前 32 位 hex，无状态校验防遍历 */
    public String shareToken(Long id) {
        return SMUtil.sm3((sm4Key == null ? "" : sm4Key.trim()) + ":dayinfo:" + id).substring(0, 32);
    }

    /** 校验公开详情访问令牌 */
    public boolean verifyShareToken(Long id, String token) {
        return id != null && token != null && constantTimeEquals(shareToken(id), token.trim());
    }

    /** 注册引导链接（base-url 下的注册页）；base-url 未配置时返回 null */
    private String registerLink() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }
        String base = baseUrl.trim();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/register.html";
    }

    /** md 转可读纯文本：去标题井号/加粗斜体/引用/代码围栏/图片，链接只留文字 */
    private static String mdToPlainText(String md) {
        if (md == null || md.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String line : md.replace("\r\n", "\n").split("\n")) {
            String l = line
                    .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "")          // 图片丢弃
                    .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1")       // 链接只留文字
                    .replaceFirst("^\\s*#{1,6}\\s*", "")                  // 标题井号
                    .replaceFirst("^\\s*>\\s?", "")                       // 引用标记
                    .replaceFirst("^\\s*`{3,}.*$", "")                    // 代码围栏行
                    .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")              // 加粗
                    .replaceAll("\\*([^*]+)\\*", "$1")                    // 斜体
                    .replace("`", "");
            out.append(l).append('\n');
        }
        return out.toString().replaceAll("\n{3,}", "\n\n").trim();
    }
}
