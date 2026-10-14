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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
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

    /** 推送通知里 md 摘要截断长度 */
    private static final int NOTIFY_SUMMARY_LEN = 200;

    @Resource
    private DayInfoMapper dayInfoMapper;

    @Resource
    private PushService pushService;

    @Value("${wxyd.dayinfo.sm2-private-key:}")
    private String sm2PrivateKey;

    @Value("${wxyd.dayinfo.sm4-key:}")
    private String sm4Key;

    /** 订阅提醒旁路发送（单线程串行，避免阻塞推送响应；失败仅记日志） */
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

        // 按 (type, info_date) upsert：同日重复推送覆盖，只留最新
        DayInfo exist = dayInfoMapper.getByTypeAndDate(type, infoDate);
        DayInfo info = new DayInfo();
        info.setType(type);
        info.setInfoDate(infoDate);
        info.setTitle(title);
        info.setContent(md);
        boolean isNew;
        if (exist == null) {
            dayInfoMapper.insert(info);
            isNew = true;
        } else {
            info.setId(exist.getId());
            dayInfoMapper.updateContent(info);
            isNew = false;
        }
        log.info("[DAYINFO] 日报入库：type={} date={} title={} mode={}", type, infoDate, title, isNew ? "insert" : "update");
        if (isNew) {
            notifySubscribers(info);
        }
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

    public List<DayInfo> list(String type, int pageNum, int pageSize) {
        String t = normalizeType(type);
        int size = Math.min(Math.max(pageSize, 1), 50);
        int page = Math.max(pageNum, 1);
        return dayInfoMapper.listByType(t, (page - 1) * size, size);
    }

    public int count(String type) {
        return dayInfoMapper.countByType(normalizeType(type));
    }

    public DayInfo detail(Long id) {
        return id == null ? null : dayInfoMapper.getById(id);
    }

    public DayInfo latest(String type) {
        return dayInfoMapper.getLatest(normalizeType(type));
    }

    /* ==================== 订阅（登录用户，一人一条） ==================== */

    public DayInfoSubscribe mySubscribe(long userId) {
        return dayInfoMapper.getSubscribeByUser(userId);
    }

    /** 保存订阅（upsert）；校验规则与羊毛雷达一致 */
    public DayInfoSubscribe saveSubscribe(long userId, String channel, Long robotId, String email, Boolean enabled) {
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
        s.setEnabled(Boolean.FALSE.equals(enabled) ? 0 : 1);
        dayInfoMapper.upsertSubscribe(s);
        return dayInfoMapper.getSubscribeByUser(userId);
    }

    /** 启用/暂停订阅；未订阅时返回 false */
    public boolean toggleSubscribe(long userId, boolean enabled) {
        return dayInfoMapper.updateSubscribeEnabled(userId, enabled ? 1 : 0) > 0;
    }

    /* ==================== 订阅提醒（旁路异步） ==================== */

    /** 新日报入库后异步提醒订阅者；发送失败仅记日志，不影响推送接口返回 */
    private void notifySubscribers(DayInfo info) {
        List<DayInfoSubscribe> subs = dayInfoMapper.listEnabledSubscribe();
        if (subs == null || subs.isEmpty()) {
            return;
        }
        String summary = info.getContent() == null ? "" : info.getContent().replaceAll("[#*`>\\[\\]]", "");
        if (summary.length() > NOTIFY_SUMMARY_LEN) {
            summary = summary.substring(0, NOTIFY_SUMMARY_LEN) + "…";
        }
        String markdown = "### " + info.getTitle() + "\n\n"
                + "「金融日报」已更新（" + info.getInfoDate() + "），登录平台查看完整内容。\n\n"
                + "---\n\n" + summary;
        for (DayInfoSubscribe s : subs) {
            notifyExecutor.execute(() -> {
                try {
                    String err = "dingtalk".equals(s.getChannel())
                            ? pushService.pushDingtalk(s.getUserId(), s.getRobotId(), "金融日报：" + info.getInfoDate(), markdown)
                            : pushService.pushEmail(s.getEmail(), "金融日报（" + info.getInfoDate() + "）",
                                    markdown.replaceAll("### ", "").replaceAll("\\*\\*", "").replaceAll("  \n", "\n"));
                    if (err != null) {
                        log.warn("[DAYINFO] 订阅提醒失败：sub={} err={}", s.getId(), err);
                    }
                } catch (Exception e) {
                    log.error("[DAYINFO] 订阅提醒异常：sub={}", s.getId(), e);
                }
            });
        }
    }
}
