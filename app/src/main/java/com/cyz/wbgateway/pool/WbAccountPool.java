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
            Map<String, WbPoolCredential> found = new LinkedHashMap<>();
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
                        String id = accountId(c);
                        WbPoolCredential prev = found.get(id);
                        if (prev == null || fresher(c, prev)) found.put(id, c);
                    } catch (IllegalStateException e) {
                        skippedFiles.add(f.getName() + "：" + e.getMessage());
                    } catch (Exception e) {
                        skippedFiles.add(f.getName() + "：读取失败 " + e.getMessage());
                    }
                }
            }

            // 合并：文件发现 + 本地 store（store 里刷新过的令牌可能比文件新）
            Map<String, WbPoolCredential> merged = new LinkedHashMap<>(found);
            for (WbPoolAccount stored : loadStore().values()) {
                WbPoolCredential sc = stored.getCredential();
                WbPoolCredential fc = merged.get(stored.getId());
                if (fc == null) {
                    if (!expiredBeyond(sc, 7L * 24 * 3600_000L)) merged.put(stored.getId(), sc);
                    continue;
                }
                if (fresher(sc, fc)) merged.put(stored.getId(), sc);
            }

            // 重建账号对象（保留池内冷却状态）
            Map<String, WbPoolAccount> next = new LinkedHashMap<>();
            List<Map.Entry<String, WbPoolCredential>> ordered = new ArrayList<>(merged.entrySet());
            ordered.sort((a, b) -> freshness(b.getValue()) - freshness(a.getValue()));
            for (Map.Entry<String, WbPoolCredential> en : ordered) {
                WbPoolAccount old = accounts.get(en.getKey());
                WbPoolAccount acc = new WbPoolAccount();
                acc.setId(en.getKey());
                acc.setCredential(en.getValue());
                acc.setLabel(label(en.getValue()));
                if (old != null) {
                    acc.setCooldownUntilMs(old.getCooldownUntilMs());
                    acc.setModelCooldowns(old.getModelCooldowns());
                    acc.setRateLimitHits(old.getRateLimitHits());
                    acc.setCredentialDeadUntilMs(old.getCredentialDeadUntilMs());
                    acc.setLastSuccessAtMs(old.getLastSuccessAtMs());
                    acc.setLastError(old.getLastError());
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

    /** 稳定账号 id：sha256("workbuddy\0" + uin|uid|nickname) 前 16 hex（计费身份，跨重登不变） */
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

    public void onSuccess(String id) {
        synchronized (lock) {
            WbPoolAccount a = accounts.get(id);
            if (a != null) a.setLastSuccessAtMs(System.currentTimeMillis());
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
            JSONArray arr = doc == null ? null : doc.getJSONArray("accounts");
            if (arr == null) return out;
            for (int i = 0; i < arr.size(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONObject c = o.getJSONObject("credential");
                if (o.getString("id") == null || c == null || c.getString("accessToken") == null) continue;
                WbPoolAccount a = new WbPoolAccount();
                a.setId(o.getString("id"));
                a.setLabel(o.getString("label"));
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
                a.setCredential(cred);
                out.put(a.getId(), a);
            }
        } catch (Exception ignore) {
            // 文件缺失/损坏：从磁盘重发现即可
        }
        return out;
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
                o.put("savedAtMs", System.currentTimeMillis());
                WbPoolCredential c = a.getCredential();
                JSONObject cj = new JSONObject();
                cj.put("accessToken", c.getAccessToken());
                cj.put("refreshToken", c.getRefreshToken());
                cj.put("uid", c.getUid());
                cj.put("enterpriseId", c.getEnterpriseId());
                cj.put("domain", c.getDomain());
                cj.put("expiresAtMs", c.getExpiresAtMs());
                cj.put("refreshExpiresAtMs", c.getRefreshExpiresAtMs());
                cj.put("lastRefreshAtMs", c.getLastRefreshAtMs());
                cj.put("nickname", c.getNickname());
                cj.put("uin", c.getUin());
                cj.put("sourcePath", c.getSourcePath());
                o.put("credential", cj);
                arr.add(o);
            }
            JSONObject doc = new JSONObject();
            doc.put("version", 1);
            doc.put("accounts", arr);
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