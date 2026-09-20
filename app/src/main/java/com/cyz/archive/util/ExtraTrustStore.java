package com.cyz.archive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 在 JVM 默认信任库之外，额外信任 {@code classpath:certs/*.pem} 里的根证书或中间证书。
 * <p>
 * 背景：目标站 kstest1.kshbank.cn 的证书链是 {@code *.kshbank.cn → CFCA OV OCA → CFCA EV ROOT}，
 * 服务器会把整条链（含自签名根）都下发。但本项目所用的 JDK 1.8.0_181 其 cacerts 共 105 个条目，
 * 其中不含任何 CFCA 根，于是抛 PKIX path building failed。
 * <p>
 * 这里<b>不关闭证书校验</b>，而是把 CFCA EV ROOT 追加为可信锚点：
 * 主机名校验、有效期校验、链完整性校验全部照旧，只是多认这一个根。
 * 想再补证书时，往 {@code resources/certs/} 放 .pem 即可，无需改代码。
 */
public final class ExtraTrustStore {

    private static final Logger log = LoggerFactory.getLogger(ExtraTrustStore.class);

    private static final String PATTERN = "classpath:certs/*.pem";

    private static volatile SslBundle cached;

    private static volatile boolean initialized;

    private ExtraTrustStore() {
    }

    /**
     * @return 组合后的 SSL 配置；若 classpath 下没有任何额外证书则返回 null（调用方继续用 JVM 默认信任库）
     */
    public static SslBundle get() {
        if (!initialized) {
            synchronized (ExtraTrustStore.class) {
                if (!initialized) {
                    cached = load();
                    initialized = true;
                }
            }
        }
        return cached;
    }

    private static SslBundle load() {
        try {
            X509TrustManager defaultTm = findX509(
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()),
                    null);
            if (defaultTm == null) {
                log.warn("未取到 JVM 默认 X509TrustManager，跳过额外根证书加载");
                return null;
            }

            KeyStore extraStore = KeyStore.getInstance(KeyStore.getDefaultType());
            extraStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(PATTERN);

            int index = 0;
            List<String> loaded = new ArrayList<>();
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    Collection<? extends Certificate> certs = cf.generateCertificates(in);
                    for (Certificate cert : certs) {
                        extraStore.setCertificateEntry("extra-" + (index++), cert);
                        if (cert instanceof X509Certificate) {
                            loaded.add(((X509Certificate) cert).getSubjectX500Principal().getName());
                        }
                    }
                } catch (Exception e) {
                    log.warn("加载额外证书失败: {}", resource.getFilename(), e);
                }
            }

            if (index == 0) {
                log.info("未发现额外根证书（{}），使用 JVM 默认信任库", PATTERN);
                return null;
            }
            log.info("已加载 {} 张额外根证书，将与 JVM 默认信任库合并: {}", index, loaded);

            X509TrustManager extraTm = findX509(
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()),
                    extraStore);
            if (extraTm == null) {
                log.warn("额外信任库初始化失败，回退 JVM 默认信任库");
                return null;
            }

            X509TrustManager composite = composite(extraTm, defaultTm);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{composite}, new SecureRandom());

            SslBundle bundle = new SslBundle();
            bundle.context = ctx;
            bundle.trustManager = composite;
            return bundle;
        } catch (Exception e) {
            log.warn("初始化额外根证书失败，回退 JVM 默认信任库", e);
            return null;
        }
    }

    private static X509TrustManager findX509(TrustManagerFactory factory, KeyStore store) throws Exception {
        factory.init(store);
        for (TrustManager tm : factory.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        return null;
    }

    /** 先按额外信任库校验，不通过再走 JVM 默认信任库 */
    private static X509TrustManager composite(X509TrustManager extra, X509TrustManager fallback) {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                try {
                    extra.checkClientTrusted(chain, authType);
                } catch (CertificateException e) {
                    fallback.checkClientTrusted(chain, authType);
                }
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                try {
                    extra.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    fallback.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] a = extra.getAcceptedIssuers();
                X509Certificate[] b = fallback.getAcceptedIssuers();
                X509Certificate[] all = new X509Certificate[a.length + b.length];
                System.arraycopy(a, 0, all, 0, a.length);
                System.arraycopy(b, 0, all, a.length, b.length);
                return all;
            }
        };
    }

    /** SSLContext 与其 X509TrustManager 的组合，okhttp 建 sslSocketFactory 时两者都要 */
    public static final class SslBundle {
        private SSLContext context;
        private X509TrustManager trustManager;

        public SSLContext getContext() {
            return context;
        }

        public X509TrustManager getTrustManager() {
            return trustManager;
        }
    }
}
