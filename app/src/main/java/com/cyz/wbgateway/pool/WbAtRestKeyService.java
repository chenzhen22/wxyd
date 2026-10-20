package com.cyz.wbgateway.pool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WorkBuddy at-rest 密钥提供者（dsh-xdpool at-rest.ts 的 Java 移植）。
 * <p>
 * 5.6.0 起桌面 App 把 accessToken/refreshToken 等写成 $wbEncrypted 信封，
 * 字段密钥 = sha256(App 内置常量 payload 的 base64 字符串)。该 payload 只能问 App 要：
 * 经 WSL interop 以批文件方式启动 WorkBuddy.exe（cmd 内 set ELECTRON_RUN_AS_NODE=1，
 * 因为 interop 不转发环境变量），跑一行内联脚本读 native binding loggerGet()。
 * 密钥按 keyId 缓存（多 build 共存时各自一条），失败负缓存 60s。
 */
@Service
@Slf4j
public class WbAtRestKeyService {

    /** App 输出会被启动日志污染，payload 用标记包裹 */
    public static final String MARK_START = "@@WBKSTART@@";
    public static final String MARK_END = "@@WBKEND@@";
    /** payload 形如 {"version":1,"atRestSecretKey":"<base64>",...}，哈希的是 base64 字符串本身 */
    private static final String INLINE_JS =
            "try{var s=process._linkedBinding('electron_browser_workbuddy_storage').loggerGet();"
                    + "process.stdout.write('" + MARK_START + "'+s+'" + MARK_END + "')}"
                    + "catch(e){process.exitCode=3;process.stderr.write(String(e&&e.message||e))}";

    @Value("${wxyd.wbgateway.workbuddy-exe:}")
    private String exeOverride;

    /** keyId -> 32 字节密钥 */
    private final Map<String, byte[]> keysById = new ConcurrentHashMap<>();
    private volatile long lastFailureAtMs;
    private volatile List<String> exeCandidates = Collections.emptyList();

    @PostConstruct
    public void init() {
        // 启动即预取一次，失败不影响网关启动（负缓存 60s）
        try {
            ensureKeys();
        } catch (Exception e) {
            log.warn("[WBPOOL] at-rest 密钥预取失败（60s 后重试）：{}", e.getMessage());
        }
    }

    /** 信封 keyId -> 密钥；取不到抛 IllegalStateException（App 未装/路径外） */
    public byte[] keyFor(String keyId) {
        byte[] k = keysById.get(keyId);
        if (k != null) return k;
        ensureKeys();
        k = keysById.get(keyId);
        if (k == null) {
            throw new IllegalStateException("WorkBuddy at-rest 密钥不可得（keyId=" + keyId
                    + "）。桌面 App 未安装或不在探测路径内，可配置 wxyd.wbgateway.workbuddy-exe 指定完整 exe 路径");
        }
        return k;
    }

    public synchronized void ensureKeys() {
        if (!keysById.isEmpty()) return;
        if (System.currentTimeMillis() - lastFailureAtMs < 60_000L) return;
        try {
            for (String exe : candidateExecutables()) {
                try {
                    JSONObject payload = fetchKeyPayload(exe);
                    String secret = payload.getString("atRestSecretKey");
                    if (secret == null || secret.isEmpty()) continue;
                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                    byte[] key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
                    keysById.put(keyId(key), key);
                    log.info("[WBPOOL] at-rest 密钥就绪：exe={} keyId={}", exe, keyId(key));
                } catch (Exception e) {
                    log.warn("[WBPOOL] 密钥获取失败 exe={}：{}", exe, brief(e.getMessage()));
                }
            }
            if (keysById.isEmpty()) lastFailureAtMs = System.currentTimeMillis();
        } catch (Exception e) {
            lastFailureAtMs = System.currentTimeMillis();
            log.warn("[WBPOOL] 密钥扫描异常：{}", e.getMessage());
        }
    }

    /** 当前已派生密钥快照（keyId -> key），auth 文档解析用；顺带触发懒加载 */
    public Map<String, byte[]> keysSnapshot() {
        ensureKeys();
        return new HashMap<>(keysById);
    }

    /** 信封 keyId（密钥 sha256 的前 16 hex） */
    public static String keyId(byte[] key) {
        try {
            StringBuilder s = new StringBuilder();
            for (byte x : MessageDigest.getInstance("SHA-256").digest(key)) s.append(String.format("%02x", x));
            return s.substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /* ---------------- exe 发现 ---------------- */

    /** 候选 exe：配置覆盖 → 注册表（powershell interop）→ 常见路径/盘符一级扫描 */
    List<String> candidateExecutables() {
        if (!exeCandidates.isEmpty()) return exeCandidates;
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (exeOverride != null && !exeOverride.trim().isEmpty()) out.add(exeOverride.trim());
        boolean onLinux = System.getProperty("os.name", "").toLowerCase().contains("linux");
        if (onLinux) {
            out.addAll(registryAppPaths());
            // 常见固定路径 + 各盘一级目录扫描（覆盖 D:\workbuddy\、D:\Program Files\WorkBuddy\ 等）
            for (char d = 'c'; d <= 'z'; d++) {
                String root = "/mnt/" + d;
                if (!new java.io.File(root).isDirectory()) continue;
                for (String mid : new String[]{"/Program Files/WorkBuddy", "/Program Files (x86)/WorkBuddy",
                        "/workbuddy", "/workbuddyai", "/WorkBuddy", "/ruanjian/anzhuang/WorkBuddy"}) {
                    out.add(root + mid + "/WorkBuddy.exe");
                    out.add(root + mid + "/WorkBuddyAI.exe");
                }
                try {
                    java.io.File rootDir = new java.io.File(root);
                    java.io.File[] subs = rootDir.listFiles(java.io.File::isDirectory);
                    if (subs != null) {
                        for (java.io.File sub : subs) {
                            String n = sub.getName();
                            if (n.equalsIgnoreCase("workbuddy") || n.equalsIgnoreCase("workbuddyai")
                                    || n.toLowerCase().contains("workbuddy") || n.toLowerCase().contains("codebuddy")) {
                                out.add(new java.io.File(sub, "WorkBuddy.exe").getAbsolutePath());
                                out.add(new java.io.File(sub, "WorkBuddyAI.exe").getAbsolutePath());
                            }
                        }
                    }
                } catch (Exception ignore) {
                }
            }
        } else {
            // Windows 本机跑 wxyd（无 WSL）：直接常见路径
            String pf = System.getenv("ProgramFiles"), pf86 = System.getenv("ProgramFiles(x86)"),
                    local = System.getenv("LOCALAPPDATA");
            for (String base : new String[]{pf, pf86, local, "D:\\workbuddy", "D:\\workbuddyai"}) {
                if (base == null) continue;
                out.add(base + "\\WorkBuddy\\WorkBuddy.exe");
                out.add(base + "\\WorkBuddyAI\\WorkBuddyAI.exe");
            }
        }
        List<String> exists = new ArrayList<>();
        for (String c : out) {
            if (c != null && new java.io.File(c).isFile()) exists.add(c);
        }
        exeCandidates = exists;
        log.info("[WBPOOL] WorkBuddy exe 候选：{}", exists);
        return exists;
    }

    /** 注册表 DisplayIcon/InstallLocation（powershell interop，编码免疫；失败返回空） */
    private List<String> registryAppPaths() {
        List<String> out = new ArrayList<>();
        try {
            String ps = "$ErrorActionPreference='SilentlyContinue';"
                    + "try { [Console]::OutputEncoding=[System.Text.Encoding]::UTF8 } catch {};"
                    + "$roots=@('HKLM:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*',"
                    + "'HKLM:\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*',"
                    + "'HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*');"
                    + "foreach($r in $roots){ Get-ItemProperty -Path $r | ForEach-Object {"
                    + " $dn=[string]$_.DisplayName; $di=[string]$_.DisplayIcon; $il=[string]$_.InstallLocation;"
                    + " if($dn -match 'WorkBuddy|CodeBuddy' -or $di -match 'WorkBuddy|CodeBuddy'){"
                    + " if($di -ne ''){ Write-Output $di } if($il -ne ''){ Write-Output $il } } } }";
            ProcessBuilder pb = new ProcessBuilder("/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe",
                    "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", ps);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = r.readLine()) != null) sb.append(line).append('\n');
            }
            if (!proc.waitFor(20, TimeUnit.SECONDS)) proc.destroyForcibly();
            for (String line : sb.toString().split("\r?\n")) {
                String s = line.trim().replaceAll("^\"", "").replaceAll("\",?-?[0-9]*$", "").replaceAll(",?-?[0-9]*$", "").trim();
                if (s.isEmpty()) continue;
                if (s.matches("(?i)[a-z]:\\\\.*")) out.add(toWslPath(s));
            }
        } catch (Exception e) {
            log.debug("[WBPOOL] 注册表查询失败（忽略）：{}", e.getMessage());
        }
        return out;
    }

    /** X:\a\b.exe -> /mnt/x/a/b.exe（空格原样保留） */
    static String toWslPath(String winPath) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(?i)([a-z]):\\\\(.*)$").matcher(winPath.trim());
        if (!m.matches()) return winPath;
        return "/mnt/" + m.group(1).toLowerCase() + "/" + m.group(2).replace('\\', '/');
    }

    /* ---------------- key payload 获取 ---------------- */

    /**
     * 批文件方式取 key payload。interop 有两个坑，均绕过：
     * 1) 环境变量不转发（WSLENV 也不可用）→ 批内 set ELECTRON_RUN_AS_NODE=1；
     * 2) Java->cmd 的命令行引号会被 interop 引号规则破坏 → 脚本写成批文件再执行。
     * 批文件放 C:\Users\Public（所有用户可写、纯 ASCII 路径），内容含 exe 路径与内联脚本。
     */
    private JSONObject fetchKeyPayload(String exePath) throws Exception {
        String winExe = toWinPath(exePath);
        if (winExe.equals(exePath)) {
            // 已经是 Windows 路径（wxyd 跑在 Windows 上）
            winExe = exePath;
        }
        String js = INLINE_JS; // 批内双引号包裹，单引号无需转义（转义反而破坏 JS 字符串）
        String batch = "@echo off\r\n"
                + "set ELECTRON_RUN_AS_NODE=1\r\n"
                + "\"" + winExe + "\" -e \"" + js + "\"\r\n";
        Path batchPath = onWsl()
                ? Paths.get("/mnt/c/Users/Public/wxyd-wbkey.cmd")
                : Paths.get("C:\\Users\\Public\\wxyd-wbkey.cmd");
        if (!new java.io.File(batchPath.getParent().toString()).isDirectory()) {
            // Public 不可用：退回系统临时目录（Windows 本机场景）
            batchPath = Paths.get(System.getProperty("java.io.tmpdir"), "wxyd-wbkey.cmd");
        }
        Files.write(batchPath, batch.getBytes(StandardCharsets.UTF_8));

        String winBatch = onWsl() ? toWinPath(batchPath.toString()) : batchPath.toString();
        ProcessBuilder pb = new ProcessBuilder(cmdExe(), "/C", winBatch);
        pb.redirectErrorStream(false);
        Process proc = pb.start();
        StringBuilder out = new StringBuilder(), err = new StringBuilder();
        Thread t1 = new Thread(() -> drain(proc.getInputStream(), out));
        Thread t2 = new Thread(() -> drain(proc.getErrorStream(), err));
        t1.start(); t2.start();
        if (!proc.waitFor(40, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            throw new IllegalStateException("App 取密钥超时（40s）exe=" + exePath);
        }
        t1.join(1000); t2.join(1000);
        if (proc.exitValue() != 0) {
            throw new IllegalStateException("App 取密钥失败 exit=" + proc.exitValue() + " " + brief(err.toString()));
        }
        String raw = out.toString();
        int bs = raw.indexOf(MARK_START), be = raw.indexOf(MARK_END);
        if (bs < 0 || be < 0 || be <= bs) {
            throw new IllegalStateException("App 输出无密钥标记（" + brief(err.toString()) + "）stdout=" + brief(raw));
        }
        return JSON.parseObject(raw.substring(bs + MARK_START.length(), be));
    }

    static String toWinPath(String p) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^/mnt/([a-zA-Z])/(.+)$").matcher(p);
        if (m.matches()) return m.group(1).toUpperCase() + ":\\" + m.group(2).replace('/', '\\');
        return p;
    }

    static boolean onWsl() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    private static String cmdExe() {
        return onWsl() ? "/mnt/c/Windows/System32/cmd.exe" : "cmd.exe";
    }

    private static void drain(java.io.InputStream in, StringBuilder sb) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line; while ((line = r.readLine()) != null) sb.append(line).append('\n');
        } catch (Exception ignore) {
        }
    }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() > 160 ? s.substring(0, 160) : s;
    }
}