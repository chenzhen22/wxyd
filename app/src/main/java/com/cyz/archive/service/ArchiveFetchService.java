package com.cyz.archive.service;

import com.cyz.archive.model.ArchiveBundle;
import com.cyz.archive.model.ArchiveException;
import com.cyz.archive.model.ArchiveFetchRequest;
import com.cyz.archive.util.ExtraTrustStore;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报文归档抓取服务。
 * <p>
 * 流程：访问目标网址 → 解析 HTML → 取 {@code id="xzz"} 元素文本 → 按 Base64 解码 → 校验 7z 魔数 → 返回字节。
 * 页面实际结构形如 {@code <span id="xzz">N3q8rycc...</span>}，除该元素外无其他内容。
 * <p>
 * 仅使用既有依赖（okhttp + jsoup），不引入新包。
 */
@Slf4j
@Service
public class ArchiveFetchService {

    /** 7z 文件魔数：37 7A BC AF 27 1C */
    private static final byte[] SEVEN_Z_MAGIC = {0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};

    /** HTML 响应体上限，防止把服务打爆 */
    private static final int MAX_HTML_BYTES = 64 * 1024 * 1024;

    private static final int DEFAULT_TIMEOUT_MS = 60_000;

    private static final String DEFAULT_ELEMENT_ID = "xzz";

    /** 保留 Base64 字符集与 URL-safe 变体，其余（空白、换行）全部去掉 */
    private static final Pattern NON_BASE64 = Pattern.compile("[^A-Za-z0-9+/=_-]");

    private static final Pattern CHARSET_PATTERN = Pattern.compile("charset\\s*=\\s*\"?([A-Za-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);

    private static final String STAGE_VALIDATE = "参数校验";
    private static final String STAGE_REQUEST = "发起请求";
    private static final String STAGE_RESPONSE = "读取响应";
    private static final String STAGE_PARSE = "解析 HTML";
    private static final String STAGE_EXTRACT = "提取元素内容";
    private static final String STAGE_DECODE = "Base64 解码";

    public ArchiveBundle fetch(ArchiveFetchRequest req) {
        long start = System.currentTimeMillis();

        if (req == null) {
            throw new ArchiveException(STAGE_VALIDATE, "请求体不能为空", 400);
        }

        String urlText = req.getUrl() == null ? "" : req.getUrl().trim();
        if (urlText.isEmpty()) {
            throw new ArchiveException(STAGE_VALIDATE, "目标网址不能为空", 400);
        }

        URI uri;
        try {
            uri = new URI(urlText);
        } catch (Exception e) {
            throw new ArchiveException(STAGE_VALIDATE, "网址格式非法：" + e.getMessage(), 400);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new ArchiveException(STAGE_VALIDATE, "仅支持 http / https 协议，当前为「" + scheme + "」", 400);
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new ArchiveException(STAGE_VALIDATE, "网址缺少主机名", 400);
        }

        String elementId = (req.getElementId() == null || req.getElementId().trim().isEmpty())
                ? DEFAULT_ELEMENT_ID : req.getElementId().trim();

        int timeoutMs = req.getTimeoutMs() == null ? DEFAULT_TIMEOUT_MS : req.getTimeoutMs();
        timeoutMs = Math.max(1000, Math.min(300_000, timeoutMs));
        boolean insecureTls = Boolean.TRUE.equals(req.getInsecureTls());

        ArchiveBundle bundle = new ArchiveBundle();
        bundle.setSourceUrl(urlText);
        bundle.setElementId(elementId);

        byte[] html = download(urlText, timeoutMs, insecureTls);
        bundle.setHtmlBytes(html.length);

        Document doc;
        try {
            doc = Jsoup.parse(new ByteArrayInputStream(html), null, urlText);
        } catch (Exception e) {
            throw new ArchiveException(STAGE_PARSE, "HTML 解析失败：" + e.getMessage(), 502);
        }

        Element el = doc.getElementById(elementId);
        if (el == null) {
            throw new ArchiveException(STAGE_EXTRACT,
                    "页面中未找到 id=\"" + elementId + "\" 的元素（已拉取 HTML "
                            + html.length + " 字节，可用 curl 直接核对页面内容）", 422);
        }
        bundle.setElementTag(el.tagName());

        String text = el.text();
        if (text == null || text.trim().isEmpty()) {
            throw new ArchiveException(STAGE_EXTRACT,
                    "id=\"" + elementId + "\" 的元素内容为空（标签 <" + el.tagName() + ">）", 422);
        }

        String cleaned = normalizeBase64(text);
        if (cleaned.isEmpty()) {
            throw new ArchiveException(STAGE_DECODE, "元素内容不含任何 Base64 字符（长度 " + text.length() + "）", 422);
        }
        bundle.setBase64Length(cleaned.length());

        byte[] data = decodeBase64(cleaned);
        bundle.setData(data);
        bundle.setDecodedSize(data.length);
        bundle.setFileName(resolveFileName(req.getFileName(), uri, data));
        fillSevenZInfo(bundle, data);
        bundle.setSha256(sha256Hex(data));
        bundle.setElapsedMs(System.currentTimeMillis() - start);

        log.info("归档抓取成功: url={} 元素={} HTML={}B Base64={}B 解码={}B 7z={} 耗时={}ms",
                urlText, elementId, html.length, cleaned.length(), data.length,
                bundle.isSevenZ(), bundle.getElapsedMs());
        return bundle;
    }

    /** 拉取页面，带超时、重定向与响应体上限 */
    private byte[] download(String urlText, int timeoutMs, boolean insecureTls) {
        OkHttpClient client = buildClient(timeoutMs, insecureTls);
        Request request = new Request.Builder()
                .url(urlText)
                .header("User-Agent", "Mozilla/5.0 (compatible; wxyd-archive/1.0)")
                .header("Accept", "text/html,application/xhtml+xml,application/xml,*/*")
                .get()
                .build();

        try (Response resp = client.newCall(request).execute()) {
            ResponseBody body = resp.body();
            if (body == null) {
                throw new ArchiveException(STAGE_RESPONSE, "上游返回空响应体", 502);
            }
            if (!resp.isSuccessful()) {
                throw new ArchiveException(STAGE_RESPONSE,
                        "上游返回 HTTP " + resp.code() + " " + resp.message(), 502);
            }
            long declared = body.contentLength();
            if (declared > MAX_HTML_BYTES) {
                throw new ArchiveException(STAGE_RESPONSE,
                        "响应体 " + declared + " 字节超过上限 " + MAX_HTML_BYTES + " 字节，已中止", 502);
            }
            return readCapped(body, MAX_HTML_BYTES);
        } catch (ArchiveException e) {
            throw e;
        } catch (IOException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            throw new ArchiveException(STAGE_REQUEST,
                    "访问目标网址失败：" + e.getClass().getSimpleName()
                            + (msg.isEmpty() ? "" : " - " + msg) + networkHint(e, msg), 502);
        }
    }

    /** 按异常类型给出可操作的排查提示，避免所有网络失败都提示「证书问题」 */
    private String networkHint(IOException e, String msg) {
        if (e instanceof SSLException || msg.contains("PKIX") || msg.contains("certificate")) {
            return "（TLS 握手失败。应用已内置 CFCA EV ROOT 根证书，若目标站用自签证书，可勾选「忽略证书校验」重试）";
        }
        if (e instanceof UnknownHostException) {
            return "（DNS 解析失败，请确认主机名正确、且本机在该内网环境内）";
        }
        if (e instanceof SocketTimeoutException) {
            return "（连接或读取超时，可适当调大超时时间后重试）";
        }
        if (e instanceof ConnectException) {
            return "（连接被拒绝，请确认目标端口已开放、服务正常）";
        }
        return "";
    }

    private byte[] readCapped(ResponseBody body, int max) throws IOException {
        try (InputStream in = body.byteStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > max) {
                    throw new ArchiveException(STAGE_RESPONSE,
                            "响应体超过上限 " + max + " 字节，已中止", 502);
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private OkHttpClient buildClient(int timeoutMs, boolean insecureTls) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMs + 10_000L, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true);

        if (insecureTls) {
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // 显式放行：仅用于内网测试环境自签证书场景
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // 同上
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, new TrustManager[]{trustManager}, new SecureRandom());
                builder.sslSocketFactory(ctx.getSocketFactory(), trustManager);
                builder.hostnameVerifier((hostname, session) -> true);
                log.warn("本次归档请求启用了「忽略证书校验」，请确认目标为内网自签证书服务");
            } catch (Exception e) {
                throw new ArchiveException(STAGE_REQUEST,
                        "初始化「忽略证书校验」的 SSLContext 失败：" + e.getMessage(), 500);
            }
        } else {
            // 默认路径：JVM 默认信任库 + classpath:certs/*.pem 里的额外根证书。
            // 仅追加可信锚点，主机名校验、有效期校验、链完整性校验全部保持开启。
            ExtraTrustStore.SslBundle ssl = ExtraTrustStore.get();
            if (ssl != null) {
                builder.sslSocketFactory(ssl.getContext().getSocketFactory(), ssl.getTrustManager());
            }
        }
        return builder.build();
    }

    /** 去掉空白，兼容 URL-safe 变体与 data URI 前缀 */
    private String normalizeBase64(String text) {
        String s = text;
        int idx = s.indexOf("base64,");
        if (idx >= 0) {
            s = s.substring(idx + "base64,".length());
        }
        s = NON_BASE64.matcher(s).replaceAll("");
        return s.replace('-', '+').replace('_', '/');
    }

    private byte[] decodeBase64(String cleaned) {
        String padded = cleaned;
        int mod = padded.length() % 4;
        if (mod == 2) {
            padded = padded + "==";
        } else if (mod == 3) {
            padded = padded + "=";
        } else if (mod == 1) {
            throw new ArchiveException(STAGE_DECODE,
                    "Base64 长度非法（" + cleaned.length() + " 字符，余 1）", 422);
        }
        try {
            return Base64.getDecoder().decode(padded);
        } catch (IllegalArgumentException e) {
            try {
                return Base64.getMimeDecoder().decode(padded);
            } catch (IllegalArgumentException e2) {
                throw new ArchiveException(STAGE_DECODE, "Base64 解码失败：" + e2.getMessage(), 422);
            }
        }
    }

    /** 校验 7z 魔数并解析头部，确认是真实可用的 7z 而非改了后缀的其它内容 */
    private void fillSevenZInfo(ArchiveBundle bundle, byte[] data) {
        if (data.length < 32 || !matchMagic(data)) {
            bundle.setSevenZ(false);
            return;
        }
        bundle.setSevenZ(true);
        bundle.setSevenZVersion((data[6] & 0xFF) + "." + (data[7] & 0xFF));
        long nextHeaderOffset = readLongLE(data, 12);
        long nextHeaderSize = readLongLE(data, 20);
        // 7z 结构自洽性：32 字节签名头 + 头部偏移 + 头部长度 == 文件总长
        bundle.setSevenZConsistent(32L + nextHeaderOffset + nextHeaderSize == data.length);
    }

    private boolean matchMagic(byte[] data) {
        for (int i = 0; i < SEVEN_Z_MAGIC.length; i++) {
            if (data[i] != SEVEN_Z_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private long readLongLE(byte[] data, int offset) {
        long v = 0;
        for (int i = 7; i >= 0; i--) {
            v = (v << 8) | (data[offset + i] & 0xFFL);
        }
        return v;
    }

    private String resolveFileName(String custom, URI uri, byte[] data) {
        if (custom != null && !custom.trim().isEmpty()) {
            String name = sanitize(custom.trim());
            return name.toLowerCase().endsWith(".7z") ? name : name + ".7z";
        }
        String host = uri.getHost() == null ? "archive" : uri.getHost();
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String suffix = data.length >= 6 && matchMagic(data) ? ".7z" : ".bin";
        return sanitize(host) + "_" + ts + suffix;
    }

    /** 去掉文件名里的路径分隔符与控制字符，避免 Content-Disposition 头注入 */
    private String sanitize(String s) {
        String cleaned = s.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        return cleaned.isEmpty() ? "archive" : cleaned;
    }

    private String sha256Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** 从 Content-Type 头里取字符集，取不到就交给 jsoup 按 <meta charset> 嗅探 */
    private String sniffCharset(String contentType) {
        if (contentType == null) {
            return null;
        }
        Matcher m = CHARSET_PATTERN.matcher(contentType);
        if (m.find()) {
            String name = m.group(1);
            try {
                return Charset.forName(name).name();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
