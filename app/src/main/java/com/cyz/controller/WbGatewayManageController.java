package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.WbGatewayKey;
import com.cyz.service.WbGatewayKeyService;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型网关管理端点（/gateway/**，登录可见，已加入 WebMvcConfig 保护清单）。
 * 每用户最多 5 把 Key；管理员专属：模型开关 + 目录免费标记视图。
 */
@RestController
@Slf4j
public class WbGatewayManageController {

    @Autowired
    private WbGatewayKeyService keyService;

    @Autowired
    private com.cyz.mapper.gateway.WbGatewayMapper wbGatewayMapper;

    /** 上游 provider 注册表（WorkBuddy / Qoder …） */
    @Autowired
    private com.cyz.wbgateway.provider.ProviderRegistry providers;


    @Autowired
    private com.cyz.config.Sm4KeyHolder sm4KeyHolder;

    /* ==================== Key 管理（登录用户本人） ==================== */

    /** 我的 Key 列表（api_key 只回掩码） */
    @GetMapping("gateway/keys")
    public Result myKeys(HttpSession session) {
        Result result = Result.getInstance();
        Long userId = userId(session);
        boolean admin = isAdmin(session); // 管理员 Key 豁免每日停用
        List<Map<String, Object>> list = new ArrayList<>();
        for (WbGatewayKey k : keyService.listByUser(userId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", k.getId());
            m.put("name", k.getName());
            m.put("model", k.getModel());
            m.put("enabled", k.getEnabled() != null && k.getEnabled() == 1);
            m.put("activeDate", k.getActiveDate());
            m.put("adminExempt", admin);
            m.put("maskedKey", maskOf(k));
            m.put("createTime", k.getCreateTime());
            m.put("lastUsedTime", k.getLastUsedTime());
            list.add(m);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("list", list);
        body.put("max", WbGatewayKey.MAX_KEYS_PER_USER);
        result.setBody(body);
        return result;
    }

    /** 掩码展示：解密 SM4 密文后取 wb-****后4位；解密失败给占位 */
    private String maskOf(WbGatewayKey k) {
        try {
            return WbGatewayKeyService.mask(sm4KeyHolder.decrypt(k.getApiKey()));
        } catch (Exception e) {
            return "wb-****";
        }
    }

    /** 新增 Key（明文只返回这一次） */
    @PostMapping("gateway/keys/add")
    public Result addKey(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            // "auto" = 跟随系统：不绑定具体模型，按请求模型（需在目录内）/系统默认模型执行
            String model = normalizeModel(str(body.get("model")));
            if (!model.isEmpty() && providers.match(model) == null) {
                throw new IllegalArgumentException("模型不在目录中：" + model);
            }
            WbGatewayKey key = keyService.create(userId(session), str(body.get("name")), model);
            Map<String, Object> out = new HashMap<>();
            out.put("id", key.getId());
            out.put("name", key.getName());
            out.put("model", key.getModel());
            out.put("apiKey", key.getApiKey()); // 明文，仅此一次
            result.setBody(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    @PostMapping("gateway/keys/delete")
    public Result deleteKey(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        if (!keyService.delete(userId(session), longVal(body.get("id")))) {
            return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        }
        return result;
    }

    @PostMapping("gateway/keys/toggle")
    public Result toggleKey(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        Boolean enabled = boolVal(body.get("enabled"));
        if (!keyService.toggle(userId(session), longVal(body.get("id")), Boolean.TRUE.equals(enabled))) {
            return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        }
        return result;
    }

    /** 查看本人 Key 明文（复制用；明文不再只在创建时显示一次） */
    @PostMapping("gateway/keys/reveal")
    public Result revealKey(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        String raw = keyService.reveal(userId(session), longVal(body.get("id")));
        if (raw == null) {
            return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        }
        result.setBody(raw);
        return result;
    }

    /** 修改 Key 的自选模型 */
    @PostMapping("gateway/keys/model")
    public Result updateKeyModel(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            String model = normalizeModel(str(body.get("model")));
            if (!model.isEmpty() && providers.match(model) == null) {
                throw new IllegalArgumentException("模型不在目录中：" + model);
            }
            if (!keyService.updateModel(userId(session), longVal(body.get("id")), model)) {
                return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /* ==================== 目录 / 状态（登录可见） ==================== */

    /** 实时模型 id 列表（WorkBuddy + Qoder 合并）+ 默认模型（不暴露免费标记） */
    @GetMapping("gateway/models")
    public Result models() {
        Result result = Result.getInstance();
        List<String> ids = providers.allModelIds();
        String dflt = providers.match(providers.defaultModel());
        if (dflt == null && !ids.isEmpty()) dflt = ids.get(0);
        Map<String, Object> body = new HashMap<>();
        body.put("models", ids);
        body.put("default", dflt == null ? "" : dflt);
        result.setBody(body);
        return result;
    }

    /** 登录态 + 积分（Windows 侧诊断；仅管理员——暴露账号级登录态与积分） */
    @GetMapping("gateway/status")
    public Result status(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        try {
            // 账号池由各 provider 自己提供展示行，控制器不关心具体厂商
            List<Map<String, Object>> pool = new ArrayList<>();
            int healthy = 0;
            for (com.cyz.wbgateway.provider.UpstreamProvider p : providers.all()) {
                pool.addAll(p.accountRows());
                healthy += Math.max(0, p.usableAccountCount());
            }
            Map<String, Object> body = new HashMap<>();
            body.put("accounts", sanitizeDeep(pool));
            body.put("poolSize", pool.size());
            body.put("healthy", healthy);
            body.put("providers", sanitizeDeep(providers.statusMap()));
            result.setBody(body);
        } catch (Exception e) {
            result.setErrorMsg("桥接不可用：" + com.cyz.wbgateway.WbGatewayChatService.sanitize(e.getMessage()));
            result.setErrorCode("000015");
        }
        return result;
    }

    /* ==================== 管理员专属 ==================== */

    /** 模型开关当前状态（仅管理员） */
    @GetMapping("gateway/admin/config")
    public Result adminConfig(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Integer sw = wbGatewayMapper.queryModelSwitch();
        Map<String, Object> body = new HashMap<>();
        body.put("modelSwitch", !Integer.valueOf(0).equals(sw));
        result.setBody(body);
        return result;
    }

    /** 切换模型开关：1=开（用户自选模型生效） 0=关（静默映射免费模型） */
    @PostMapping("gateway/admin/switch")
    public Result adminSwitch(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Map<String, Object> body = body(req);
        Boolean on = boolVal(body.get("on"));
        if (on == null) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg("缺少 on 参数");
            return result;
        }
        wbGatewayMapper.updateModelSwitch(Boolean.TRUE.equals(on) ? 1 : 0);
        log.info("[WBGATEWAY] 模型开关切换为 {}（by user {}）", on, userId(session));
        return result;
    }

    /** 免费模型优先级：自动顺序 / 手动配置 / 实际生效（仅管理员） */
    @GetMapping("gateway/admin/free-priority")
    public Result freePriority(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        List<String> auto = new ArrayList<>();
        for (com.cyz.wbgateway.provider.UpstreamProvider p : providers.all()) {
            for (String id : providers.publicCatalog().freeIdsOf(p)) {
                if (!auto.contains(id)) auto.add(id);
            }
        }
        String csv = wbGatewayMapper.queryFreePriority();
        Map<String, Object> body = new HashMap<>();
        body.put("auto", auto);
        body.put("manual", com.cyz.wbgateway.provider.FreePriority.parse(csv));
        body.put("effective", com.cyz.wbgateway.provider.FreePriority.order(auto, csv));
        body.put("invalid", com.cyz.wbgateway.provider.FreePriority.invalid(auto, csv));
        result.setBody(body);
        return result;
    }

    /** 保存免费模型优先级（逗号分隔或数组；留空=恢复自动） */
    @PostMapping("gateway/admin/free-priority")
    public Result saveFreePriority(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Object raw = body(req).get("list");
        List<String> items = new ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) if (o != null) items.add(String.valueOf(o).trim());
        } else if (raw != null) {
            items = com.cyz.wbgateway.provider.FreePriority.parse(String.valueOf(raw));
        }
        List<String> clean = new ArrayList<>();
        for (String s : items) if (!s.isEmpty() && !clean.contains(s)) clean.add(s);
        String csv = String.join(",", clean);
        wbGatewayMapper.updateFreePriority(csv);
        log.info("[WBGATEWAY] 免费模型优先级已更新：{}（by user {}）", csv, userId(session));
        result.setBody(freePriority(session).getBody());
        return result;
    }

    /** 目录全量视图（含免费标记，仅管理员——用于核对开关映射目标） */
    @GetMapping("gateway/admin/models")
    public Result adminModels(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        com.cyz.wbgateway.provider.CompositeCatalog catalog = providers.publicCatalog();
        Map<String, com.cyz.wbgateway.provider.UpstreamProvider> byId = new HashMap<>();
        for (com.cyz.wbgateway.provider.UpstreamProvider p : providers.all()) byId.put(p.id(), p);
        List<Map<String, Object>> list = new ArrayList<>();
        for (com.cyz.wbgateway.provider.CompositeCatalog.Group g : catalog.getGroups()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", g.display);
            row.put("name", g.display);
            boolean free = false;
            for (Map.Entry<String, String> e : g.nativeByProvider.entrySet()) {
                com.cyz.wbgateway.provider.UpstreamProvider p = byId.get(e.getKey());
                if (p != null && p.isFree(e.getValue())) { free = true; break; }
            }
            row.put("free", free);
            row.put("providers", String.join(",", g.nativeByProvider.keySet()));
            list.add(row);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("list", list);
        body.put("source", "live");
        result.setBody(body);
        return result;
    }

    /** 全部用户的 Key 列表（仅管理员） */
    @GetMapping("gateway/admin/keys")
    public Result adminKeys(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Map<String, Object> body = new HashMap<>();
        body.put("list", keyService.adminListKeys());
        result.setBody(body);
        return result;
    }

    /**
     * 设置单条 Key 的模型开关（仅管理员）。
     * override: auto=跟随全局（NULL） on=强制开（1） off=强制关（0）
     */
    @PostMapping("gateway/admin/keys/switch")
    public Result adminKeySwitch(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Map<String, Object> body = body(req);
        String ov = str(body.get("override"));
        Integer val;
        if ("on".equalsIgnoreCase(ov)) val = 1;
        else if ("off".equalsIgnoreCase(ov)) val = 0;
        else if ("auto".equalsIgnoreCase(ov)) val = null;
        else {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg("override 取值须为 auto | on | off");
            return result;
        }
        Long id = longVal(body.get("id"));
        if (id == null || !keyService.adminSetModelOverride(id, val)) {
            return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        }
        log.info("[WBGATEWAY] Key 级模型开关：id={} -> {}（by user {}）", id, ov, userId(session));
        return result;
    }

    /* ==================== 令牌消耗统计（仅管理员） ==================== */

    /** 调用明细（请求/应答 JSON）存放目录（与 WbGatewayChatService 同配置项） */
    @Value("${wxyd.wbgateway.usage-detail-dir:/apps/shareFile/gateway}")
    private String usageDetailDir;

    /** 汇总：总量 + 今日 + 按用户 + 按模型 */
    @GetMapping("gateway/admin/usage/summary")
    public Result usageSummary(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Map<String, Object> body = new HashMap<>();
        body.put("totals", wbGatewayMapper.usageTotals());
        body.put("today", wbGatewayMapper.usageTotalsToday());
        body.put("byUser", wbGatewayMapper.usageByUser());
        body.put("byModel", wbGatewayMapper.usageByModel());
        result.setBody(body);
        return result;
    }

    /** 明细日志（分页，支持用户/日期筛选；date 格式 yyyy-MM-dd） */
    @GetMapping("gateway/admin/usage/logs")
    public Result usageLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "20") Integer pageSize,
                            @RequestParam(required = false) Long userId,
                            @RequestParam(required = false) String date,
                            HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        String day = null;
        if (date != null && !date.trim().isEmpty()) {
            try {
                day = java.time.LocalDate.parse(date.trim()).toString();
            } catch (Exception e) {
                result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
                result.setErrorMsg("日期格式需为 yyyy-MM-dd");
                return result;
            }
        }
        Map<String, Object> body = new HashMap<>();
        body.put("total", wbGatewayMapper.usageLogsCount(userId, day));
        body.put("list", wbGatewayMapper.usageLogs(userId, day, (pn - 1) * ps, ps));
        body.put("pageNum", pn);
        body.put("pageSize", ps);
        result.setBody(body);
        return result;
    }

    /** 单条调用详情（含请求/应答数据，仅管理员）。明细从 /apps/shareFile/gateway/{id}.json 读，DB 旧列为存量回退 */
    @GetMapping("gateway/admin/usage/detail")
    public Result usageDetail(@RequestParam Long id, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Map<String, Object> detail = wbGatewayMapper.usageDetail(id);
        if (detail == null) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000022);
        // 明细文件优先（新数据 DB 不存大字段）；读不到回退 DB 旧列（存量记录）
        JSONObject fileDoc = readDetailFile(id);
        if (fileDoc != null) {
            detail.put("requestData", fileDoc.getString("requestData"));
            detail.put("responseData", fileDoc.getString("responseData"));
        }
        result.setBody(detail);
        return result;
    }

    private JSONObject readDetailFile(Long id) {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(usageDetailDir, id + ".json");
            if (!java.nio.file.Files.exists(p)) return null;
            return com.alibaba.fastjson2.JSON.parseObject(
                    new String(java.nio.file.Files.readAllBytes(p), java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("[WBGATEWAY] 调用明细文件读取失败 id={}：{}", id, e.getMessage());
            return null;
        }
    }

    /* ==================== 积分自动化（仅管理员） ==================== */

    @Autowired
    private com.cyz.wbgateway.pool.WbCreditAutomationService automationService;

    @GetMapping("gateway/admin/automation")
    public Result automationStatus(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        result.setBody(sanitizeDeep(automationService.statusView()));
        return result;
    }

    /** 手动立即执行一个自动化任务（不消耗当日定时档位） */
    @PostMapping("gateway/admin/automation/run")
    public Result automationRun(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        String kind = req != null && req.getBody() instanceof Map
                ? String.valueOf(((Map<?, ?>) req.getBody()).get("kind")) : "";
        try {
            result.setBody(sanitizeDeep(automationService.runNow(kind.trim())));
        } catch (IllegalArgumentException e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        } catch (Exception e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg("执行失败：" + e.getMessage());
        }
        return result;
    }

    /** 一键执行全部自动化任务（串行，约 1 分钟；不消耗当日定时档位） */
    @PostMapping("gateway/admin/automation/runAll")
    public Result automationRunAll(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        try {
            result.setBody(sanitizeDeep(automationService.runAllNow()));
        } catch (Exception e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg("执行失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("gateway/admin/automation/enabled")
    public Result automationEnabled(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        boolean on = req != null && req.getBody() instanceof Map
                && Boolean.TRUE.equals(((Map<?, ?>) req.getBody()).get("enabled"));
        automationService.setEnabled(on);
        return result;
    }

    /* ==================== 浏览器扫码登录（仅管理员） ==================== */

    @Autowired
    private com.cyz.wbgateway.WbBrowserLoginService browserLoginService;

    /** 发起浏览器登录：返回登录链接（管理员在本机浏览器打开并扫码） */
    @PostMapping("gateway/admin/browser-login/start")
    public Result browserLoginStart(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Map<String, Object> body = body(req);
        String product = str(body.get("product"));
        if (product == null || product.isEmpty()) product = "buddy";
        try {
            com.cyz.wbgateway.WbBrowserLoginService.Session s = browserLoginService.start(product.trim());
            Map<String, Object> out = new HashMap<>();
            out.put("sessionId", s.id);
            out.put("authUrl", s.authUrl);
            result.setBody(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 轮询登录进度（前端每 2s 一次）；authorized 时账号已注入池 */
    @PostMapping("gateway/admin/browser-login/poll")
    public Result browserLoginPoll(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Map<String, Object> body = body(req);
        try {
            com.cyz.wbgateway.WbBrowserLoginService.Session s =
                    browserLoginService.poll(str(body.get("sessionId")));
            Map<String, Object> out = new HashMap<>();
            out.put("status", s.status);
            out.put("message", s.message);
            result.setBody(out);
        } catch (IllegalStateException e) {
            result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 取消登录会话 */
    @PostMapping("gateway/admin/browser-login/cancel")
    public Result browserLoginCancel(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) return result.setErrorEnum(com.cyz.constant.ErrorEnum.ERROR000014);
        Map<String, Object> body = body(req);
        browserLoginService.cancel(str(body.get("sessionId")));
        return result;
    }

    /* ==================== 辅助 ==================== */

    private static Long userId(HttpSession session) {
        Long id = (Long) session.getAttribute("userId");
        return id == null ? 0L : id;
    }

    private static boolean isAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role != null && role == 0;
    }

    /** 模型取值归一：null/空白/"auto"/"system" 均表示「跟随系统」 */
    private static String normalizeModel(String model) {
        if (model == null) return "";
        String v = model.trim();
        if (v.isEmpty() || "auto".equalsIgnoreCase(v) || "system".equalsIgnoreCase(v)) return "";
        return v;
    }

    /** 应答脱敏：递归替换上游厂商字样（管理页诊断信息也不暴露） */
    private static Object sanitizeDeep(Object o) {
        if (o instanceof String) {
            return ((String) o).replaceAll("(?i)workbuddy|qoder(-cn)?", "上游服务");
        }
        if (o instanceof Map) {
            Map<Object, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                out.put(e.getKey(), sanitizeDeep(e.getValue()));
            }
            return out;
        }
        if (o instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object v : (List<?>) o) out.add(sanitizeDeep(v));
            return out;
        }
        return o;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long longVal(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean boolVal(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.valueOf(String.valueOf(o));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(Result req) {
        if (req == null || !(req.getBody() instanceof Map)) {
            return new HashMap<>();
        }
        return (Map<String, Object>) req.getBody();
    }
}
