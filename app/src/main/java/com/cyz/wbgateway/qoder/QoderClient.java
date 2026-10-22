package com.cyz.wbgateway.qoder;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.wbgateway.WbUpstreamException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Qoder（国内版）上游客户端：COSY 签名 + 置换 base64 请求体 + 双层 SSE。
 * <p>
 * 协议细节照 dsh-connect-qoder 0.7.4 逆向移植（签名头 / Encode=1 编码 / 信封解包）。
 * 凭据优先读本机 Qoder 桌面应用登录态（Windows OSCrypt + DPAPI，经 WSL interop 解锁）。
 */
@Component
@Slf4j
public class QoderClient {

    /* ==================== 协议常量 ==================== */

    private static final String RSA_PEM = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDA8iMH5c02LilrsERw9t6Pv5Nc"
            + "4k6Pz1EaDicBMpdpxKduSZu5OANqUq8er4GM95omAGIOPOh+Nx0spthYA2BqGz+l"
            + "6HRkPJ7S236FZz73In/KVuLnwI8JJ2CbuJap8kvheCCZpmAWpb/cPx/3Vr/J6I17"
            + "XcW+ML9FoCI6AOvOzwIDAQAB";
    private static final String CUSTOM_ALPHABET = "_doRTgHZBKcGVjlvpC,@aFSx#DPuNJme&i*MzLOEn)sUrthbf%Y^w.(kIQyXqWA!";
    private static final String STD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final char[] ENCODE = new char[256];
    private static final String COSY_VERSION = "1.1.38";
    private static final String BASE_URL = "https://gateway.qoder.com.cn/";
    private static final String MODEL_ID_PREFIX = "qoder:";

    static {
        for (int i = 0; i < 256; i++) ENCODE[i] = (char) i;
        for (int i = 0; i < 64; i++) ENCODE[STD_ALPHABET.charAt(i)] = CUSTOM_ALPHABET.charAt(i);
        ENCODE['='] = '$';
    }

    /* ==================== 配置 ==================== */

    @Value("${wxyd.qoder.enabled:true}")
    private boolean enabled;

    /** 可选：直接指定 auth.v1.dat 路径（WSL 路径） */
    @Value("${wxyd.qoder.auth-file:}")
    private String authFileOverride;

    /** 可选：直接指定访问令牌（配合 wxyd.qoder.user-id），跳过桌面登录态读取 */
    @Value("${wxyd.qoder.token:}")
    private String tokenOverride;

    @Value("${wxyd.qoder.user-id:}")
    private String uidOverride;

    @Value("${wxyd.qoder.temperature-models:}")
    private String extraModelKeys;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build();

    private static final SecureRandom RANDOM = new SecureRandom();

    private volatile QoderCatalog cachedCatalog;
    private volatile long catalogAt;
    private volatile Credential cachedCred;
    private volatile long credAt;

    /* ==================== 凭据 ==================== */

    public static class Credential {
        public String uid = "";
        public String token = "";
        public String name = "";
        public String machineId = "";
        public String source = "";
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** 当前凭据（缓存 5 分钟；读不到返回 null） */
    public synchronized Credential credential() {
        if (cachedCred != null && System.currentTimeMillis() - credAt < 5 * 60_000L) return cachedCred;
        Credential c = null;
        try {
            if (notBlank(tokenOverride) && notBlank(uidOverride)) {
                c = new Credential();
                c.uid = uidOverride.trim();
                c.token = tokenOverride.trim();
                c.name = "config";
                c.source = "config";
            } else {
                c = loadDesktopCredential();
            }
        } catch (Exception e) {
            log.warn("[QODER] 读取登录态失败：{}", e.getMessage());
        }
        cachedCred = c;
        credAt = System.currentTimeMillis();
        return c;
    }

    public synchronized void invalidateCredential() {
        cachedCred = null;
        credAt = 0;
    }

    /**
     * 读 Qoder 桌面应用登录态：auth.v1.dat（Chromium OSCrypt v10）+ Local State 里的 DPAPI 密钥。
     * DPAPI 只有 Windows 能解，本应用跑在 WSL，故经 powershell.exe 取 32 字节 AES key。
     */
    private Credential loadDesktopCredential() throws Exception {
        File auth = findAuthFile();
        if (auth == null) return null;
        File appDir = auth.getParentFile();
        String winDir = toWindowsPath(appDir.getAbsolutePath());
        String keyB64 = unwrapOscryptKey(winDir);
        if (keyB64 == null || keyB64.isEmpty()) return null;
        byte[] aesKey = Base64.getDecoder().decode(keyB64.trim());
        byte[] blob = Files.readAllBytes(auth.toPath());
        if (blob.length < 31 || !"v10".equals(new String(blob, 0, 3, StandardCharsets.US_ASCII))) {
            log.warn("[QODER] auth.v1.dat 不是 v10 格式");
            return null;
        }
        byte[] nonce = java.util.Arrays.copyOfRange(blob, 3, 15);
        byte[] tag = java.util.Arrays.copyOfRange(blob, blob.length - 16, blob.length);
        byte[] ct = java.util.Arrays.copyOfRange(blob, 15, blob.length - 16);
        byte[] all = new byte[ct.length + tag.length];
        System.arraycopy(ct, 0, all, 0, ct.length);
        System.arraycopy(tag, 0, all, ct.length, tag.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, nonce));
        String plain = new String(cipher.doFinal(all), StandardCharsets.UTF_8);

        JSONObject session = JSON.parseObject(plain);
        if (session == null) return null;
        String token = session.getString("token");
        JSONObject user = session.getJSONObject("user");
        String uid = user == null ? "" : user.getString("id");
        if (!notBlank(token) || !notBlank(uid)) return null;
        Credential c = new Credential();
        c.uid = uid;
        c.token = token;
        c.name = user.getString("name") == null ? "" : user.getString("name");
        c.machineId = readMachineId(appDir);
        c.source = "desktop";
        log.info("[QODER] 已读取桌面登录态 uid={} expires={}", uid, session.getString("expiresAt"));
        return c;
    }

    private File findAuthFile() {
        if (notBlank(authFileOverride)) {
            File f = new File(authFileOverride.trim());
            if (f.isFile()) return f;
            log.warn("[QODER] 配置的 auth-file 不存在：{}", authFileOverride);
        }
        // WSL 下按 Windows 用户目录扫描（新版布局 com.qodercn.app.stable；兼容旧版 QoderCN）
        File users = new File("/mnt/c/Users");
        File[] dirs = users.listFiles();
        if (dirs == null) return null;
        String[] apps = {"com.qodercn.app.stable", "QoderCN", "Qoder CN"};
        for (File u : dirs) {
            for (String app : apps) {
                File f = new File(new File(new File(u, "AppData/Roaming"), app), "auth.v1.dat");
                if (f.isFile()) return f;
            }
        }
        return null;
    }

    private String readMachineId(File appDir) {
        for (String n : new String[]{"auth.machine-id", "machineid", "machineId"}) {
            File f = new File(appDir, n);
            try {
                if (f.isFile()) {
                    String v = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
                    if (!v.isEmpty()) return v;
                }
            } catch (Exception ignore) {
            }
        }
        return "";
    }

    /** /mnt/c/Users/x/... → C:\Users\x\... */
    private static String toWindowsPath(String wslPath) {
        if (!wslPath.startsWith("/mnt/") || wslPath.length() < 7) return wslPath;
        char drive = wslPath.charAt(5);
        String rest = wslPath.substring(7).replace('/', '\\');
        return Character.toUpperCase(drive) + ":\\" + rest;
    }

    /** powershell 解 DPAPI，输出 Local State 里 OSCrypt 主密钥的 base64 */
    private String unwrapOscryptKey(String winAppDir) throws IOException, InterruptedException {
        String script = "Add-Type -TypeDefinition @'\n"
                + "using System; using System.Runtime.InteropServices;\n"
                + "public class QD {\n"
                + " [StructLayout(LayoutKind.Sequential)] public struct B { public int cbData; public IntPtr pbData; }\n"
                + " [DllImport(\"Crypt32.dll\", SetLastError=true)] static extern bool CryptUnprotectData(ref B i, IntPtr d, IntPtr e, IntPtr r, IntPtr p, int f, ref B o);\n"
                + " [DllImport(\"Kernel32.dll\")] static extern IntPtr LocalFree(IntPtr h);\n"
                + " public static byte[] U(byte[] data) {\n"
                + "  B i = new B(); i.cbData = data.Length; i.pbData = Marshal.AllocHGlobal(data.Length);\n"
                + "  Marshal.Copy(data, 0, i.pbData, data.Length);\n"
                + "  B o = new B();\n"
                + "  try {\n"
                + "   if (!CryptUnprotectData(ref i, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero, 0, ref o)) throw new Exception(\"DPAPI \" + Marshal.GetLastWin32Error());\n"
                + "   byte[] r = new byte[o.cbData]; Marshal.Copy(o.pbData, r, 0, o.cbData); return r;\n"
                + "  } finally { Marshal.FreeHGlobal(i.pbData); if (o.pbData != IntPtr.Zero) LocalFree(o.pbData); }\n"
                + " }\n"
                + "}\n"
                + "'@\n"
                + "$j = Get-Content (Join-Path '" + winAppDir + "' 'Local State') -Raw | ConvertFrom-Json\n"
                + "$raw = [Convert]::FromBase64String($j.os_crypt.encrypted_key)\n"
                + "$key = [QD]::U($raw[5..($raw.Length-1)])\n"
                + "[Convert]::ToBase64String($key)\n";
        byte[] utf16 = script.getBytes(StandardCharsets.UTF_16LE);
        String encoded = Base64.getEncoder().encodeToString(utf16);
        ProcessBuilder pb = new ProcessBuilder("/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe",
                "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        boolean done = p.waitFor(30, TimeUnit.SECONDS);
        if (!done) {
            p.destroyForcibly();
            log.warn("[QODER] DPAPI 解锁超时");
            return null;
        }
        for (String line : sb.toString().split("\\R")) {
            String t = line.trim();
            if (t.length() == 44 && t.endsWith("=")) return t;
        }
        log.warn("[QODER] DPAPI 解锁失败：{}", sb.toString().trim());
        return null;
    }

    /* ==================== 模型目录 ==================== */

    public synchronized QoderCatalog models() {
        if (cachedCatalog != null && System.currentTimeMillis() - catalogAt < 10 * 60_000L) return cachedCatalog;
        Credential cred = credential();
        if (cred == null) {
            return cachedCatalog == null ? QoderCatalog.empty() : cachedCatalog;
        }
        try {
            String url = BASE_URL + "algo/api/v2/model/list?Encode=1";
            Map<String, String> headers = authHeaders(new byte[0], url, cred);
            Request req = new Request.Builder().url(url)
                    .header("Accept", "application/json")
                    .headers(okhttp3.Headers.of(headers))
                    .get().build();
            try (Response resp = http.newCall(req).execute()) {
                String text = resp.body() == null ? "" : resp.body().string();
                if (!resp.isSuccessful()) {
                    throw new WbUpstreamException(classify(resp.code(), text), resp.code(), excerpt(text));
                }
                JSONObject data = JSON.parseObject(text);
                JSONArray chat = data == null ? null : data.getJSONArray("chat");
                QoderCatalog cat = QoderCatalog.parse(chat, MODEL_ID_PREFIX);
                cachedCatalog = cat;
                catalogAt = System.currentTimeMillis();
                log.info("[QODER] 模型目录已刷新：{} 个模型", cat.ids().size());
                return cat;
            }
        } catch (Exception e) {
            log.warn("[QODER] 拉取模型目录失败：{}", e.getMessage());
            return cachedCatalog == null ? QoderCatalog.empty() : cachedCatalog;
        }
    }

    /* ==================== 对话 ==================== */

    /**
     * 发起一次流式对话，返回未消费的上游响应（SSE）。body 为 OpenAI chat-completions 请求体。
     * 调用方负责关闭。
     */
    public Response chat(String upstreamModelKey, JSONObject body, boolean alwaysThinking) throws WbUpstreamException {
        Credential cred = credential();
        if (cred == null) {
            throw new WbUpstreamException(WbUpstreamException.KIND_SESSION_DEAD, 401, "Qoder 未登录：请在本机 Qoder 桌面端登录");
        }
        JSONObject req = buildBody(upstreamModelKey, body, alwaysThinking);
        byte[] encoded = encodeBody(req.toJSONString().getBytes(StandardCharsets.UTF_8));
        String url = BASE_URL + "algo/api/v2/service/pro/sse/agent_chat_generation"
                + "?FetchKeys=llm_model_result&AgentId=agent_common&Encode=1";
        Map<String, String> headers;
        try {
            headers = authHeaders(encoded, url, cred);
        } catch (Exception e) {
            throw new WbUpstreamException(WbUpstreamException.KIND_SERVER, 0, "签名失败: " + e.getMessage());
        }
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "text/event-stream");
        headers.put("Cache-Control", "no-cache");
        headers.put("Accept-Encoding", "identity");
        headers.put("X-Model-Key", upstreamModelKey);
        headers.put("X-Model-Source", "system");
        Request request = new Request.Builder().url(url)
                .headers(okhttp3.Headers.of(headers))
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), encoded))
                .build();
        try {
            Response resp = http.newCall(request).execute();
            if (resp.isSuccessful()) return resp;
            String text = resp.body() == null ? "" : resp.body().string();
            int code = resp.code();
            resp.close();
            if (code == 401) invalidateCredential();
            throw new WbUpstreamException(classify(code, text), code, excerpt(text));
        } catch (WbUpstreamException e) {
            throw e;
        } catch (Exception e) {
            throw new WbUpstreamException(WbUpstreamException.KIND_SERVER, 0, "transport error: " + e.getMessage());
        }
    }

    /** OpenAI 请求体 → Qoder 的 agent_chat_generation 请求体 */
    private JSONObject buildBody(String modelKey, JSONObject body, boolean alwaysThinking) {
        JSONArray messages = body.getJSONArray("messages");
        JSONArray tools = body.getJSONArray("tools");
        String lastText = "";
        if (messages != null) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                JSONObject m = messages.getJSONObject(i);
                if (m != null && "user".equals(m.getString("role"))) {
                    Object c = m.get("content");
                    lastText = c instanceof String ? (String) c : JSON.toJSONString(c);
                    break;
                }
            }
        }
        JSONObject parameters = new JSONObject();
        Long maxTokens = body.getLong("max_tokens");
        if (maxTokens != null && maxTokens > 0) parameters.put("max_tokens", maxTokens);
        if (alwaysThinking) {
            parameters.put("enable_thinking", true);
            String effort = body.getString("reasoning_effort");
            parameters.put("reasoning_effort", notBlank(effort) ? effort : "low");
        } else {
            parameters.put("enable_thinking", false);
        }

        JSONObject extra = new JSONObject();
        extra.put("context", new JSONArray());
        JSONObject modelConfig = new JSONObject();
        modelConfig.put("key", modelKey);
        modelConfig.put("is_reasoning", alwaysThinking);
        extra.put("modelConfig", modelConfig);
        extra.put("originalContent", lastText);
        JSONObject chatContext = new JSONObject();
        chatContext.put("chatPrompt", "");
        chatContext.put("imageUrls", null);
        chatContext.put("extra", extra);
        chatContext.put("features", new JSONArray());
        chatContext.put("text", lastText);

        JSONObject business = new JSONObject();
        business.put("product", "cli");
        business.put("version", "1.0.0");
        business.put("type", "agent");
        business.put("stage", "start");
        business.put("id", java.util.UUID.randomUUID().toString());
        business.put("name", lastText.length() > 30 ? lastText.substring(0, 30) : lastText);
        business.put("begin_at", System.currentTimeMillis());

        String rid = java.util.UUID.randomUUID().toString();
        JSONObject out = new JSONObject();
        out.put("request_id", java.util.UUID.randomUUID().toString());
        out.put("request_set_id", rid);
        out.put("chat_record_id", rid);
        out.put("session_id", "dsh-" + java.util.UUID.randomUUID());
        out.put("stream", true);
        out.put("chat_task", "FREE_INPUT");
        out.put("is_reply", true);
        out.put("is_retry", false);
        out.put("source", 1);
        out.put("version", "3");
        out.put("session_type", "qodercli");
        out.put("agent_id", "agent_common");
        out.put("task_id", "common");
        out.put("code_language", "");
        out.put("chat_prompt", "");
        out.put("image_urls", null);
        out.put("aliyun_user_type", "");
        out.put("system", "");
        out.put("messages", messages == null ? new JSONArray() : messages);
        out.put("tools", tools == null ? new JSONArray() : tools);
        out.put("parameters", parameters);
        out.put("chat_context", chatContext);
        JSONObject mc = new JSONObject();
        mc.put("key", modelKey);
        mc.put("source", "system");
        mc.put("is_reasoning", alwaysThinking);
        out.put("model_config", mc);
        out.put("business", business);
        return out;
    }

    /* ==================== COSY 签名 / 编码 ==================== */

    private Map<String, String> authHeaders(byte[] body, String url, Credential cred) throws Exception {
        String aesKey = randomHex(16);
        JSONObject info = new JSONObject();
        info.put("uid", cred.uid);
        info.put("security_oauth_token", cred.token);
        info.put("name", cred.name == null ? "" : cred.name);
        info.put("aid", "");
        info.put("email", "");
        String infoB64 = aesCbcBase64(info.toJSONString(), aesKey);

        PublicKey pub = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(RSA_PEM)));
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, pub);
        String cosyKey = Base64.getEncoder().encodeToString(rsa.doFinal(aesKey.getBytes(StandardCharsets.US_ASCII)));

        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        JSONObject payload = new JSONObject();
        payload.put("version", "v1");
        payload.put("requestId", java.util.UUID.randomUUID().toString());
        payload.put("info", infoB64);
        payload.put("cosyVersion", COSY_VERSION);
        payload.put("ideVersion", "");
        String payloadB64 = Base64.getEncoder().encodeToString(payload.toJSONString().getBytes(StandardCharsets.UTF_8));

        String path = new java.net.URL(url).getPath();
        if (path.startsWith("/algo")) path = path.substring(5);

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(payloadB64.getBytes(StandardCharsets.UTF_8));
        md5.update((byte) '\n');
        md5.update(cosyKey.getBytes(StandardCharsets.UTF_8));
        md5.update((byte) '\n');
        md5.update(ts.getBytes(StandardCharsets.UTF_8));
        md5.update((byte) '\n');
        md5.update(body);
        md5.update((byte) '\n');
        md5.update(path.getBytes(StandardCharsets.UTF_8));
        String sig = hex(md5.digest());

        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "Bearer COSY." + payloadB64 + "." + sig);
        h.put("Cosy-Key", cosyKey);
        h.put("Cosy-User", cred.uid);
        h.put("Cosy-Date", ts);
        h.put("Cosy-Version", COSY_VERSION);
        h.put("Cosy-Machineid", cred.machineId == null ? "" : cred.machineId);
        h.put("Cosy-Machinetoken", cred.machineId == null ? "" : cred.machineId);
        h.put("Cosy-Machinetype", "5");
        h.put("Cosy-Machineos", "x86_64_windows");
        h.put("Cosy-Clienttype", "5");
        h.put("Cosy-Clientip", "127.0.0.1");
        h.put("Cosy-Bodyhash", hex(MessageDigest.getInstance("MD5").digest(body)));
        h.put("Cosy-Bodylength", String.valueOf(body.length));
        h.put("Cosy-Sigpath", path);
        h.put("Cosy-Data-Policy", "disagree");
        h.put("Cosy-Organization-Id", "");
        h.put("Cosy-Organization-Tags", "");
        h.put("Login-Version", "v2");
        h.put("X-Request-Id", java.util.UUID.randomUUID().toString());
        return h;
    }

    /** base64 → 置换字母表 → 按三分之一旋转 */
    public static byte[] encodeBody(byte[] plain) {
        String std = Base64.getEncoder().encodeToString(plain);
        int n = std.length();
        int third = n / 3;
        StringBuilder sb = new StringBuilder(n);
        for (int i = n - third; i < n; i++) sb.append(ENCODE[std.charAt(i) & 0xff]);
        for (int i = third; i < n - third; i++) sb.append(ENCODE[std.charAt(i) & 0xff]);
        for (int i = 0; i < third; i++) sb.append(ENCODE[std.charAt(i) & 0xff]);
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String aesCbcBase64(String plain, String key) throws Exception {
        byte[] k = key.getBytes(StandardCharsets.US_ASCII);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, "AES"), new IvParameterSpec(k));
        return Base64.getEncoder().encodeToString(c.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    private static String randomHex(int len) {
        byte[] b = new byte[len];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.substring(0, len);
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    /* ==================== 错误分类 ==================== */

    public static String classify(int status, String body) {
        String text = body == null ? "" : body;
        if (status == 401) return WbUpstreamException.KIND_SESSION_DEAD;
        if (status == 402) return WbUpstreamException.KIND_HARD_CREDIT;
        if (status == 429 || status == 10605) return WbUpstreamException.KIND_SOFT_RATE;
        if (text.contains("TOKEN_EXPIRE") || text.contains("\"105\"")) return WbUpstreamException.KIND_SESSION_DEAD;
        if (status == 404) return WbUpstreamException.KIND_NOT_FOUND;
        if (status >= 500) return WbUpstreamException.KIND_SERVER;
        return WbUpstreamException.KIND_CLIENT;
    }

    static String excerpt(String text) {
        if (text == null) return "";
        String t = text.trim();
        return t.length() > 500 ? t.substring(0, 500) : t;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** 模型 id 前缀（qoder:） */
    public static String prefix() {
        return MODEL_ID_PREFIX;
    }

    /* 兼容旧调用 */
    public List<String> extraModels() {
        List<String> out = new ArrayList<>();
        if (extraModelKeys != null) {
            for (String s : extraModelKeys.split(",")) if (!s.trim().isEmpty()) out.add(s.trim());
        }
        return out;
    }
}
