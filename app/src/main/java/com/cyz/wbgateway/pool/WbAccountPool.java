package com.cyz.wbgateway.pool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WorkBuddy 多账号池（dsh-xdpool accounts.ts 的 Java 移植核心）。
 * <p>
 * 扫描桌面 App 的 auth 目录（live 文件 + 历史快照 .info），按计费身份合并去重；
 * 与本地持久化 store（data/wb-pool/accounts.json）双向合并，刷新出的令牌跨重启存活；
 * pick() 跳过冷却账号实现自动换号。纯 Java，无 node 依赖。
 */
@Service
@Slf4j
public class WbAccountPool {

    private static final String LIVE_FILENAME = "workbuddy-desktop.info";
    private static final long SCAN_TTL_MS = 5 * 60_000L;
    /** 429 按模型冷却 */
    private static final long MODEL_COOLDOWN_MS = 60_000L;
    /** 积分耗尽账号级冷却 */
    private static final long CREDIT_COOLDOWN_MS = 5 * 60_000L;
    /** 登录态被上游判死：只有重登能解，冷却要长 */
    private static final long DEAD_COOLDOWN_MS = 30 * 60_000L;

    private final WbAtRestKeyService keyService;

    @Value("${wxyd.wbgateway.pool-scan-dir:}")
    private String scanDirOverride;

    /** accountId -> 账号（freshness 降序） */
    private final Map<String, WbPoolAccount> accounts = new LinkedHashMap<>();
    private final Object lock = new Object();
    private volatile long lastScanAt;
    private final List<String> skippedFiles = new ArrayList<>();
    /** 管理员手动删除的账号 id：持久化在 store 里，重扫不再捞回（同账号重新扫码登录会解除） */
    private final java.util.Set<String> excluded = new java.util.LinkedHashSet<>();

    public WbAccountPool(WbAtRestKeyService keyService) {
        this.keyService = keyService;
    }

    @PostConstruct
    public void init() {
        try {
            scan();
        } catch (Exception e) {
            log.warn("[WBPOOL] 启动扫描失败（首次请求时重试）：{}", e.getMessage());
        }
    }

    /* ---------------- 扫描与合并 ---------------- */

    /** 重扫 auth 目录并合并池（TTL 内直接复用） */
    public void scanIfNeeded() {
        if (System.currentTimeMillis() - lastScanAt < SCAN_TTL_MS) return;
        synchronized (lock) {
            if (System.currentTimeMillis() - lastScanAt < SCAN_TTL_MS) return;
            scan();
        }
    }

    public void scan() {
        synchronized (lock) {
            // id -> 该账号发现到的全部凭据（桌面 App 与浏览器登录各一份，按 freshness 降序；同一 token 只留一份）
            Map<String, List<WbPoolCredential>> found = new LinkedHashMap<>();
            skippedFiles.clear();
            for (String dir : authDirs()) {
                File d = new File(dir);
                File[] files = d.listFiles((f, n) -> n.endsWith(".info"));
                if (files == null) continue;
                for (File f : files) {
                    try {
                        String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                        WbPoolCredential c = WbAuthParser.parse(text, f.getAbsolutePath(),
                                f.getName().equals(LIVE_FILENAME), keyService.keysSnapshot());
                        if (c == null) continue;
                        addCredential(found, accountId(c), c);
                    } catch (IllegalStateException e) {
                        skippedFiles.add(f.getName() + "：" + e.getMessage());
                    } catch (Exception e) {
                        skippedFiles.add(f.getName() + "：读取失败 " + e.getMessage());
                    }
                }
            }

            // 合并：文件发现 + 本地 store（store 里刷新过的令牌可能比文件新）
            // 同 id 的全部凭据都保留（主用一份、其余作备用），主凭据被判死时可轮换，不会像之前那样把可用令牌丢掉
            Map<String, List<WbPoolCredential>> merged = new LinkedHashMap<>();
            for (Map.Entry<String, List<WbPoolCredential>> en : found.entrySet()) {
                for (WbPoolCredential c : en.getValue()) addCredential(merged, en.getKey(), c);
            }
            for (WbPoolAccount stored : loadStore().values()) {
                for (WbPoolCredential sc : stored.allCredentials()) {
                    // 按凭据重算 id（不信任存储里的旧 key）：老的 uin 口径账号在这里自动迁到 uid 口径，不会残留在池里
                    String sid = accountId(sc);
                    if (merged.containsKey(sid) || !expiredBeyond(sc, 7L * 24 * 3600_000L)) {
                        addCredential(merged, sid, sc);
                    }
                }
            }

            // 手动删除过的账号不再自动回池（loadStore 已把排除表读进 excluded）
            if (!excluded.isEmpty()) merged.keySet().removeAll(excluded);

            // 保持已判定的主凭据顺序：否则每 5 分钟重扫会把刚轮换掉的死令牌又排回主位，白付一次 401+刷新
            for (Map.Entry<String, List<WbPoolCredential>> en : merged.entrySet()) {
                WbPoolAccount old = accounts.get(en.getKey());
                if (old != null) stableOrder(en.getValue(), old.allCredentials());
            }

            // 重建账号对象（保留池内冷却状态）
            Map<String, WbPoolAccount> next = new LinkedHashMap<>();
            List<Map.Entry<String, List<WbPoolCredential>>> ordered = new ArrayList<>(merged.entrySet());
            ordered.sort((a, b) -> freshness(b.getValue().get(0)) - freshness(a.getValue().get(0)));
            for (Map.Entry<String, List<WbPoolCredential>> en : ordered) {
                WbPoolAccount old = accounts.get(en.getKey());
                WbPoolAccount acc = new WbPoolAccount();
                acc.setId(en.getKey());
                acc.setCredential(en.getValue().get(0));
                acc.setAlternates(new ArrayList<>(en.getValue().subList(1, en.getValue().size())));
                acc.setLabel(label(en.getValue().get(0)));
                if (old != null) {
                    acc.setCooldownUntilMs(old.getCooldownUntilMs());
                    acc.setModelCooldowns(old.getModelCooldowns());
                    acc.setRateLimitHits(old.getRateLimitHits());
                    acc.setCredentialDeadUntilMs(old.getCredentialDeadUntilMs());
                    acc.setLastSuccessAtMs(old.getLastSuccessAtMs());
                    acc.setLastError(old.getLastError());
                    acc.setEnabled(old.isEnabled());
                }
                next.put(en.getKey(), acc);
            }
            accounts.clear();
            accounts.putAll(next);
            lastScanAt = System.currentTimeMillis();
            saveStore();
            log.info("[WBPOOL] 扫描完成：{} 个账号，{} 个文件跳过", accounts.size(), skippedFiles.size());
        }
    }

    /** 已入池凭据按原顺序排前（主凭据/轮换结果不被重扫打乱），新出现的凭据按 freshness 追加在后 */
    private static void stableOrder(List<WbPoolCredential> list, List<WbPoolCredential> knownOrder) {
        List<WbPoolCredential> reordered = new ArrayList<>();
        for (WbPoolCredential k : knownOrder) {
            for (WbPoolCredential c : list) {
                if (k.getAccessToken() != null && k.getAccessToken().equals(c.getAccessToken())
                        && !reordered.contains(c)) {
                    reordered.add(c);
                }
            }
        }
        for (WbPoolCredential c : list) if (!reordered.contains(c)) reordered.add(c);
        list.clear();
        list.addAll(reordered);
    }

    /** 按 id 收集凭据（同 token 去重），列表始终按 freshness 降序，index 0 即主用凭据 */
    private static void addCredential(Map<String, List<WbPoolCredential>> into, String id, WbPoolCredential c) {
        List<WbPoolCredential> list = into.computeIfAbsent(id, k -> new ArrayList<>());
        for (WbPoolCredential e : list) {
            if (c.getAccessToken() != null && c.getAccessToken().equals(e.getAccessToken())) return;
        }
        list.add(c);
        list.sort((a, b) -> freshness(b) - freshness(a));
    }

    /** freshness 降序：live > lastRefreshTime > expiresAt */
    private static int freshness(WbPoolCredential c) {
        int s = c.isLive() ? 2_000_000_000 : 0;
        s += (int) Math.min(1_000_000_000L, c.getLastRefreshAtMs() == null ? 0 : c.getLastRefreshAtMs() / 1000);
        s += (int) Math.min(500_000_000L, c.getExpiresAtMs() / 2000);
        return s;
    }

    private static boolean fresher(WbPoolCredential a, WbPoolCredential b) {
        return freshness(a) > freshness(b);
    }

    private static boolean expiredBeyond(WbPoolCredential c, long graceMs) {
        long ref = c.getRefreshExpiresAtMs() != null ? c.getRefreshExpiresAtMs()
                : (c.getExpiresAtMs() == null ? 0 : c.getExpiresAtMs());
        return ref > 0 && ref < System.currentTimeMillis() - graceMs;
    }

    /**
     * 稳定账号 id：sha256("workbuddy\0" + uin|uid|nickname) 前 16 hex（计费身份，跨重登不变）。
     * <p>
     * 故意不做跨来源归并：桌面 App 快照（有 uin）与浏览器扫码登录（只有 uid）各自成号，
     * 同一个人的两份令牌互为兜底（一份被判死还能换另一份）。不想要的那条在「模型管理 → 账号池」手动删除。
     */
    public static String accountId(WbPoolCredential c) {
        String stable = firstNonEmpty(c.getUin(), c.getUid(), c.getNickname(), "unknown");
        try {
            java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
            return hex(sha.digest(("workbuddy\0" + stable).getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String label(WbPoolCredential c) {
        String name = firstNonEmpty(c.getNickname(), "WorkBuddy");
        String disc = firstNonEmpty(c.getUid(), c.getUin(), "");
        if (disc.length() > 8) disc = disc.substring(0, 8);
        return disc.isEmpty() ? name : name + "#" + disc;
    }

    private static String firstNonEmpty(String... xs) {
        for (String x : xs) if (x != null && !x.isEmpty()) return x;
        return "";
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x));
        return s.toString();
    }

    /* ---------------- 选号 ---------------- */

    /**
     * 选一个可用账号（跳过冷却/会话死者，按 freshness 顺序）。
     * 无可用账号抛 IllegalStateException。
     */
    public WbPoolAccount pick(String model, Set<String> excludeIds) {
        scanIfNeeded();
        synchronized (lock) {
            long now = System.currentTimeMillis();
            for (WbPoolAccount a : accounts.values()) {
                if (excludeIds != null && excludeIds.contains(a.getId())) continue;
                if (!a.available(now, model)) continue;
                return a;
            }
        }
        throw new IllegalStateException("账号池无可用账号（共 " + size() + " 个，均被冷却/判死或被排除）");
    }

    /* ---------------- 失败/成功记账 ---------------- */

    public void onModelRateLimited(String id, String model) {
        synchronized (lock) {
            WbPoolAccount a = accounts.get(id);
            if (a == null) return;
            a.getModelCooldowns().put(model, System.currentTimeMillis() + MODEL_COOLDOWN_MS);
            a.setRateLimitHits(a.getRateLimitHits() + 1);
            log.info("[WBPOOL] 账号 {} 模型 {} 限流冷却 {}s", a.getLabel(), model, MODEL_COOLDOWN_MS / 1000);
        }
    }

    public void onHardCredit(String id) {
        synchronized (lock) {
            WbPoolAccount a = accounts.get(id);
            if (a == null) return;
            a.setCooldownUntilMs(System.currentTimeMillis() + CREDIT_COOLDOWN_MS);
            a.setLastError("积分耗尽");
            log.info("[WBPOOL] 账号 {} 积分耗尽，账号级冷却 {}min", a.getLabel(), CREDIT_COOLDOWN_MS / 60000);
        }
    }

    public void onCredentialDead(String id, String reason) {
        synchronized (lock) {
            WbPoolAccount a = accounts.get(id);
            if (a == null) return;
            a.setCredentialDeadUntilMs(System.currentTimeMillis() + DEAD_COOLDOWN_MS);
            a.setLastError("登录态失效：" + reason);
            log.warn("[WBPOOL] 账号 {} 登录态被上游判死，冷却 {}min：{}", a.getLabel(), DEAD_COOLDOWN_MS / 60000, reason);
        }
    }

    /** 刷新成功：更新凭据 + 清全部冷却 + 持久化 */
    public void onRefreshed(String id, WbPoolCredential refreshed) {
        synchronized (lock) {
            WbPoolAccount a = accounts.get(id);
            if (a == null) return;
            refreshed.setLive(a.getCredential().isLive());
            refreshed.setSourcePath(a.getCredential().getSourcePath());
            a.setCredential(refreshed);
            a.setCooldownUntilMs(0);
            a.setCredentialDeadUntilMs(0);
            a.setModelCooldowns(new LinkedHashMap<>());
            a.setLastError(null);
            saveStore();
            log.info("[WBPOOL] 账号 {} 令牌已刷新并清冷却", a.getLabel());
        }
    }

    /**
     * 注入新凭据（浏览器扫码登录用）：按计费身份算 accountId——同 uid 重登会顶掉
     * 旧凭据并清全部冷却（判死账号复活），新 uid 则入池为新账号；立刻持久化。
     */
    public void inject(WbPoolCredential credential) {
        String id = accountId(credential);
        synchronized (lock) {
            excluded.remove(id);  // 重新登录 = 明确要用这个号，解除之前的删除标记
            WbPoolAccount old = accounts.get(id);
            WbPoolAccount acc = new WbPoolAccount();
            acc.setId(id);
            acc.setCredential(credential);
            acc.setLabel(label(credential));
            acc.setLastSuccessAtMs(old == null ? 0 : old.getLastSuccessAtMs());
            acc.setEnabled(old == null || old.isEnabled());  // 重登覆盖凭据时保留管理员开关
            if (old != null) {
                // 旧凭据降为备用：新登录的令牌未必比旧的活得久，留着判死时可轮换
                List<WbPoolCredential> alts = new ArrayList<>();
                for (WbPoolCredential c : old.allCredentials()) {
                    if (c.getAccessToken() != null && !c.getAccessToken().equals(credential.getAccessToken())) alts.add(c);
                }
                acc.setAlternates(alts);
            }
            accounts.remove(id);          // 重排：新凭据按 freshness 排到最前
            accounts.put(id, acc);
            lastScanAt = System.currentTimeMillis(); // 防止 TTL 重扫抢先覆盖
            saveStore();
            log.info("[WBPOOL] 账号 {} 已注入（{}）", acc.getLabel(),
                    old == null ? "新账号" : "覆盖旧凭据并清冷却");
        }
    }

    /**
     * 主凭据被上游判死（401/刷新失败）：轮换到同账号的下一个凭据并清冷却。
     * 只有一份凭据时返回 false，由调用方按「账号失效」处理。
     */
    public boolean rotateCredential(String id) {
        synchronized (lock) {
            WbPoolAccount a = accounts.get(id);
            if (a == null) return false;
            List<WbPoolCredential> all = a.allCredentials();
            if (all.size() < 2) return false;
            WbPoolCredential first = all.remove(0);
            all.add(first);
            a.setCredential(all.get(0));
            a.setAlternates(new ArrayList<>(all.subList(1, all.size())));
            a.setCredentialDeadUntilMs(0);
            a.setCooldownUntilMs(0);
            a.setModelCooldowns(new LinkedHashMap<>());
            a.setLastError(null);
            saveStore();
            log.info("[WBPOOL] 账号 {} 主凭据失效，轮换到备用凭据（本号共 {} 份）", a.getLabel(), all.size());
            return true;
        }
    }

    public void onSuccess(String id) {
        synchronized (lock) {
            WbPoolAccount a = accounts.get(id);
            if (a != null) a.setLastSuccessAtMs(System.currentTimeMillis());
        }
    }

    /**
     * 管理员手动删除账号：移出池并记进排除表（持久化），之后重扫 auth 目录也不会再捞回来；
     * 同一账号重新扫码登录（inject）会解除排除。
     */
    public boolean remove(String id) {
        synchronized (lock) {
            WbPoolAccount a = accounts.remove(id);
            if (a == null) return false;
            excluded.add(id);
            saveStore();
            log.info("[WBPOOL] 账号 {} 已从池中删除（{} 个排除项，重扫不再自动加入）", a.getLabel(), excluded.size());
            return true;
        }
    }

    /** 管理员开关：false = 模型调用不再使用该账号（pick 跳过），状态持久化 */
    public boolean setEnabled(String id, boolean enabled) {
        synchronized (lock) {
            WbPoolAccount a = accounts.get(id);
            if (a == null) return false;
            a.setEnabled(enabled);
            saveStore();
            log.info("[WBPOOL] 账号 {} 已{}", a.getLabel(), enabled ? "启用" : "停用（模型调用不再使用）");
            return true;
        }
    }

    /* ---------------- 视图/持久化 ---------------- */

    public int size() {
        synchronized (lock) { return accounts.size(); }
    }

    public List<String> skipped() {
        synchronized (lock) { return new ArrayList<>(skippedFiles); }
    }

    /** 池概览（管理页/状态接口用，不暴露 token） */
    public List<Map<String, Object>> statusRows() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (WbPoolAccount a : accounts.values()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", a.getId());
                m.put("label", a.getLabel());
                m.put("domain", a.getCredential().getDomain());
                m.put("expiresAtMs", a.getCredential().getExpiresAtMs());
                m.put("lastRefreshAtMs", a.getCredential().getLastRefreshAtMs());
                m.put("lastSuccessAtMs", a.getLastSuccessAtMs() <= 0 ? null : a.getLastSuccessAtMs());
                m.put("cooldownUntilMs", a.getCooldownUntilMs() > now ? a.getCooldownUntilMs() : null);
                m.put("credentialDeadUntilMs", a.getCredentialDeadUntilMs() > now ? a.getCredentialDeadUntilMs() : null);
                m.put("rateLimitHits", a.getRateLimitHits());
                m.put("lastError", a.getLastError());
                m.put("source", a.getCredential().isLive() ? "live" : "snapshot");
                m.put("enabled", a.isEnabled());
                rows.add(m);
            }
            return rows;
        }
    }

    public WbPoolAccount get(String id) {
        synchronized (lock) { return accounts.get(id); }
    }

    /* ---------------- 本地 store（原子写） ---------------- */

    private Path storePath() {
        return Paths.get(System.getProperty("user.dir"), "data", "wb-pool", "accounts.json");
    }

    private Map<String, WbPoolAccount> loadStore() {
        Map<String, WbPoolAccount> out = new LinkedHashMap<>();
        try {
            JSONObject doc = JSON.parseObject(new String(Files.readAllBytes(storePath()), StandardCharsets.UTF_8));
            JSONArray ex = doc == null ? null : doc.getJSONArray("excluded");
            if (ex != null) {
                excluded.clear();
                for (int i = 0; i < ex.size(); i++) if (ex.getString(i) != null) excluded.add(ex.getString(i));
            }
            JSONArray arr = doc == null ? null : doc.getJSONArray("accounts");
            if (arr == null) return out;
            for (int i = 0; i < arr.size(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.getString("id") == null) continue;
                List<WbPoolCredential> creds = new ArrayList<>();
                JSONArray list = o.getJSONArray("credentials");
                if (list != null) {
                    for (int k = 0; k < list.size(); k++) {
                        WbPoolCredential c = credFromJson(list.getJSONObject(k));
                        if (c != null && c.getAccessToken() != null) creds.add(c);
                    }
                } else {
                    // 旧格式：单 credential 字段
                    WbPoolCredential c = credFromJson(o.getJSONObject("credential"));
                    if (c != null && c.getAccessToken() != null) creds.add(c);
                }
                if (creds.isEmpty()) continue;
                WbPoolAccount a = new WbPoolAccount();
                a.setId(o.getString("id"));
                a.setLabel(o.getString("label"));
                a.setCredential(creds.get(0));
                a.setAlternates(new ArrayList<>(creds.subList(1, creds.size())));
                Boolean en = o.getBoolean("enabled");
                a.setEnabled(en == null || en);  // 旧 store 无该字段默认启用
                out.put(a.getId(), a);
            }
        } catch (Exception ignore) {
            // 文件缺失/损坏：从磁盘重发现即可
        }
        return out;
    }

    private static JSONObject credToJson(WbPoolCredential c) {
        JSONObject j = new JSONObject();
        j.put("accessToken", c.getAccessToken());
        j.put("refreshToken", c.getRefreshToken());
        j.put("uid", c.getUid());
        j.put("enterpriseId", c.getEnterpriseId());
        j.put("domain", c.getDomain());
        j.put("expiresAtMs", c.getExpiresAtMs());
        j.put("refreshExpiresAtMs", c.getRefreshExpiresAtMs());
        j.put("lastRefreshAtMs", c.getLastRefreshAtMs());
        j.put("nickname", c.getNickname());
        j.put("uin", c.getUin());
        j.put("sourcePath", c.getSourcePath());
        return j;
    }

    private static WbPoolCredential credFromJson(JSONObject c) {
        if (c == null) return null;
        WbPoolCredential cred = new WbPoolCredential();
        cred.setAccessToken(c.getString("accessToken"));
        cred.setRefreshToken(c.getString("refreshToken"));
        cred.setUid(c.getString("uid"));
        cred.setEnterpriseId(c.getString("enterpriseId"));
        cred.setDomain(c.getString("domain") == null ? "" : c.getString("domain"));
        cred.setExpiresAtMs(c.getLongValue("expiresAtMs"));
        cred.setRefreshExpiresAtMs(c.getLong("refreshExpiresAtMs"));
        cred.setLastRefreshAtMs(c.getLong("lastRefreshAtMs"));
        cred.setNickname(c.getString("nickname"));
        cred.setUin(c.getString("uin"));
        cred.setSourcePath(c.getString("sourcePath"));
        return cred;
    }

    private void saveStore() {
        try {
            Path p = storePath();
            Files.createDirectories(p.getParent());
            JSONArray arr = new JSONArray();
            for (WbPoolAccount a : accounts.values()) {
                JSONObject o = new JSONObject();
                o.put("id", a.getId());
                o.put("label", a.getLabel());
                o.put("enabled", a.isEnabled());
                o.put("savedAtMs", System.currentTimeMillis());
                JSONArray creds = new JSONArray();
                for (WbPoolCredential c : a.allCredentials()) creds.add(credToJson(c));
                o.put("credentials", creds);
                o.put("credential", creds.get(0));  // 兼容旧读法
                arr.add(o);
            }
            JSONObject doc = new JSONObject();
            doc.put("version", 1);
            doc.put("accounts", arr);
            doc.put("excluded", new JSONArray(new ArrayList<>(excluded)));
            Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
            Files.write(tmp, doc.toJSONString().getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log.warn("[WBPOOL] 账号 store 写入失败（不影响运行）：{}", e.getMessage());
        }
    }

    /* ---------------- auth 目录发现 ---------------- */

    /** 桌面 App auth 目录（xdpool defaultDesktopAuthDirs 的跨平台 Java 版；WSL 下扫 /mnt/c/Users/*） */
    List<String> authDirs() {
        if (scanDirOverride != null && !scanDirOverride.trim().isEmpty()) {
            return Arrays.asList(scanDirOverride.trim().split("\\s*,\\s*"));
        }
        List<String> out = new ArrayList<>();
        boolean onLinux = System.getProperty("os.name", "").toLowerCase().contains("linux");
        if (onLinux) {
            File users = new File("/mnt/c/Users");
            File[] dirs = users.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File u : dirs) {
                    String n = u.getName();
                    if (n.equals("Public") || n.equals("Default") || n.equals("Default User")
                            || n.equals("All Users") || n.endsWith(".ini")) continue;
                    out.add(new File(u, "AppData/Local/CodeBuddyExtension/Data/Public/auth").getAbsolutePath());
                    out.add(new File(u, "AppData/Roaming/CodeBuddyExtension/Data/Public/auth").getAbsolutePath());
                }
            }
        } else {
            String home = System.getProperty("user.home");
            String local = System.getenv("LOCALAPPDATA");
            String roaming = System.getenv("APPDATA");
            String base1 = (local != null ? local : home + "/AppData/Local").replace('\\', '/');
            String base2 = (roaming != null ? roaming : home + "/AppData/Roaming").replace('\\', '/');
            out.add(base1 + "/CodeBuddyExtension/Data/Public/auth");
            out.add(base2 + "/CodeBuddyExtension/Data/Public/auth");
        }
        return out;
    }
}