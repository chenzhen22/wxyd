package com.cyz.patrol.service;

import com.cyz.config.Sm4KeyHolder;
import com.cyz.patrol.model.PatrolAsset;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 巡检探测器：url 存活（顺带 SSL 到期）/ domain WHOIS 到期 / server SSH 磁盘内存。
 * 全部纯 Java 实现；任何异常都归一化为 CheckResult，不向上抛。
 */
@Service
@Slf4j
public class PatrolProbe {

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build();

    /** 主流注册商的到期日期字段（取首个命中） */
    private static final Pattern WHOIS_EXPIRY = Pattern.compile(
            "(?:Registry Expiry Date|Expiration Date|Expiry Date|Expiration Time)\\s*[:：]\\s*(\\d{4}-\\d{2}-\\d{2})");

    @Resource
    private Sm4KeyHolder sm4KeyHolder;

    public PatrolAsset.CheckResult check(PatrolAsset asset) {
        PatrolAsset.CheckResult r = new PatrolAsset.CheckResult();
        r.setAssetId(asset.getId());
        r.setName(asset.getName());
        r.setAssetType(asset.getAssetType());
        r.setTarget(asset.getTarget());
        try {
            switch (asset.getAssetType() == null ? "" : asset.getAssetType()) {
                case "url": checkUrl(asset, r); break;
                case "domain": checkDomain(asset, r); break;
                case "server": checkServer(asset, r); break;
                default:
                    r.setStatus("unknown");
                    r.setDetail("未知资产类型：" + asset.getAssetType());
            }
        } catch (Exception e) {
            log.warn("[PATROL] 探测异常：{} / {}", asset.getName(), e.getMessage());
            r.setStatus("fail");
            r.setDetail("探测异常：" + brief(e.getMessage()));
        }
        return r;
    }

    /** url 存活 + HTTPS 顺带查证书剩余天数 */
    private void checkUrl(PatrolAsset a, PatrolAsset.CheckResult r) throws Exception {
        String target = a.getTarget();
        Request req = new Request.Builder().url(target)
                .header("User-Agent", "wxyd-patrol/1.0").head().build();
        long start = System.currentTimeMillis();
        try (Response resp = HTTP.newCall(req).execute()) {
            int code = resp.code();
            long cost = System.currentTimeMillis() - start;
            boolean ok = code >= 200 && code < 400;
            r.setStatus(ok ? "ok" : "fail");
            r.setDetail("HTTP " + code + " · " + cost + "ms");
            // HTTPS 证书剩余天数（同一次探测顺带完成，不单独建资产）
            if (target.startsWith("https://")) {
                int days = sslDaysLeft(target);
                if (days >= 0) {
                    r.setDetail(r.getDetail() + " · 证书剩 " + days + " 天");
                    if (days < 7) r.setStatus("fail");
                    else if (days < 30 && "ok".equals(r.getStatus())) r.setStatus("warn");
                }
            }
        }
    }

    private int sslDaysLeft(String url) {
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();
            conn.disconnect();
            if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                Date notAfter = ((X509Certificate) certs[0]).getNotAfter();
                long days = (notAfter.getTime() - System.currentTimeMillis()) / 86400000L;
                return (int) days;
            }
        } catch (Exception e) {
            log.debug("[PATROL] SSL 读取失败（不影响存活判定）：{}", e.getMessage());
        }
        return -1;
    }

    /** domain WHOIS 到期（Socket 43，主流注册商正则；解析失败=unknown 不误报） */
    private void checkDomain(PatrolAsset a, PatrolAsset.CheckResult r) throws Exception {
        String domain = a.getTarget().trim().toLowerCase();
        String whois;
        try (Socket sock = new Socket()) {
            sock.connect(new java.net.InetSocketAddress("whois.iana.org", 43), 5000);
            sock.setSoTimeout(5000);
            java.io.OutputStream out = sock.getOutputStream();
            out.write((domain + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            whois = sb.toString();
        }
        // IANA 返回 refer 指向注册商 WHOIS 服务器时需二次查询
        Matcher refer = Pattern.compile("(?i)whois:\\s*(\\S+)").matcher(whois);
        if (refer.find()) {
            String server = refer.group(1);
            try (Socket sock = new Socket()) {
                sock.connect(new java.net.InetSocketAddress(server, 43), 5000);
                sock.setSoTimeout(5000);
                sock.getOutputStream().write((domain + "\r\n").getBytes(StandardCharsets.UTF_8));
                sock.getOutputStream().flush();
                BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                whois = sb.toString();
            }
        }
        Matcher m = WHOIS_EXPIRY.matcher(whois);
        if (!m.find()) {
            r.setStatus("unknown");
            r.setDetail("WHOIS 未解析出到期日（注册商响应格式未覆盖），可人工确认");
            return;
        }
        java.time.LocalDate expiry = java.time.LocalDate.parse(m.group(1));
        long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), expiry);
        r.setDetail("域名 " + expiry + " 到期，剩 " + days + " 天");
        if (days < 7) r.setStatus("fail");
        else if (days < 30) r.setStatus("warn");
        else r.setStatus("ok");
    }

    /** server SSH：磁盘（根分区）+ 内存使用率；≥80 warn、≥90 fail */
    private void checkServer(PatrolAsset a, PatrolAsset.CheckResult r) throws Exception {
        if (a.getSshUser() == null || a.getSshPassword() == null) {
            r.setStatus("unknown");
            r.setDetail("未配置 SSH 用户/密码");
            return;
        }
        JSch jsch = new JSch();
        Session session = jsch.getSession(a.getSshUser(), a.getTarget(),
                a.getPort() == null ? 22 : a.getPort());
        session.setPassword(sm4KeyHolder.decrypt(a.getSshPassword()));
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "password");
        session.connect(6000);
        try {
            String disk = exec(session, "df -h / | awk 'NR==2{print $5}'");
            String mem = exec(session, "free -m | awk 'NR==2{printf \"%.0f\", $3/$2*100}'");
            int diskPct = parsePct(disk);
            int memPct = parsePct(mem);
            if (diskPct < 0 || memPct < 0) {
                r.setStatus("unknown");
                r.setDetail("命令输出解析失败（disk=" + brief(disk) + " mem=" + brief(mem) + "）");
                return;
            }
            int worst = Math.max(diskPct, memPct);
            r.setDetail("磁盘 " + diskPct + "% · 内存 " + memPct + "%");
            if (worst >= 90) r.setStatus("fail");
            else if (worst >= 80) r.setStatus("warn");
            else r.setStatus("ok");
        } finally {
            session.disconnect();
        }
    }

    private String exec(Session session, String cmd) throws Exception {
        ChannelExec ch = (ChannelExec) session.openChannel("exec");
        ch.setCommand(cmd);
        ch.setErrStream(null);
        InputStream in = ch.getInputStream();
        ch.connect(4000);
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[256];
        int n;
        while ((n = in.read(buf)) != -1) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        ch.disconnect();
        return sb.toString().trim();
    }

    private static int parsePct(String s) {
        try {
            return Integer.parseInt(s.trim().replace("%", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private static String brief(String msg) {
        if (msg == null) return "";
        return msg.length() > 120 ? msg.substring(0, 120) : msg;
    }
}
