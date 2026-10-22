package com.cyz.wbgateway.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** provider 注册表：按模型 id 路由到对应适配器。 */
@Component
@Slf4j
public class ProviderRegistry {

    @Autowired(required = false)
    private List<UpstreamProvider> providers = new ArrayList<>();

    /** 合并目录（同名去重）缓存 60s；余额/可用性评分缓存 10 分钟（不每次请求都刷余额） */
    private volatile CompositeCatalog pubCatalog;
    private volatile long pubAt;
    private final Map<String, long[]> scoreCache = new ConcurrentHashMap<>();
    private static final long SCORE_TTL_MS = 10 * 60_000L;

    public List<UpstreamProvider> all() {
        return providers;
    }

    /** 按模型 id 找 provider（未命中返回第一个，保证有兜底） */
    public UpstreamProvider providerFor(String modelId) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("没有可用的上游 provider");
        }
        if (modelId != null) {
            for (UpstreamProvider p : providers) {
                if (p.owns(modelId)) return p;
            }
        }
        return providers.get(0);
    }

    /** 合并目录（同名模型只出现一次） */
    public CompositeCatalog publicCatalog() {
        long now = System.currentTimeMillis();
        CompositeCatalog c = pubCatalog;
        if (c == null || now - pubAt > 60_000L) {
            c = CompositeCatalog.build(providers, null);
            pubCatalog = c;
            pubAt = now;
        }
        return c;
    }

    /** 一次路由结果：用哪个 provider + 它的原生模型 id + 对外展示 id */
    public static class Route {
        public final UpstreamProvider provider;
        public final String nativeId;
        public final String displayId;

        public Route(UpstreamProvider provider, String nativeId, String displayId) {
            this.provider = provider;
            this.nativeId = nativeId;
            this.displayId = displayId;
        }
    }

    /**
     * 解析展示模型 → provider。同名模型按「账号可用/余额充足」评分择优（评分缓存 10 分钟）。
     */
    public Route resolve(String requested) {
        CompositeCatalog cat = publicCatalog();
        CompositeCatalog.Group g = cat.find(requested);
        if (g == null) {
            UpstreamProvider p = providers.isEmpty() ? null : providers.get(0);
            return new Route(p, requested, requested);
        }
        UpstreamProvider best = null;
        long bestScore = Long.MIN_VALUE;
        for (UpstreamProvider p : providers) {
            String nativeId = g.nativeByProvider.get(p.id());
            if (nativeId == null) continue;
            long s = scoreOf(p);
            if (s > bestScore) {
                bestScore = s;
                best = p;
            }
        }
        if (best == null) best = providers.get(0);
        return new Route(best, g.nativeByProvider.get(best.id()), g.display);
    }

    /** 同一模型的可选 provider（按可用性评分降序）；模型不在任何目录时返回全部 provider */
    public List<UpstreamProvider> candidates(String display) {
        CompositeCatalog.Group g = publicCatalog().find(display);
        List<UpstreamProvider> list = new ArrayList<>();
        if (g != null) {
            for (UpstreamProvider p : providers) {
                if (g.nativeByProvider.containsKey(p.id())) list.add(p);
            }
        } else {
            list.addAll(providers);
        }
        list.sort((a, b) -> Long.compare(scoreOf(b), scoreOf(a)));
        return list;
    }

    /** provider 可用性评分：可用账号越多/余额越高越优先；-1 表示不可用 */
    private long scoreOf(UpstreamProvider p) {
        long now = System.currentTimeMillis();
        long[] cached = scoreCache.get(p.id());
        if (cached != null && now - cached[1] < SCORE_TTL_MS) return cached[0];
        long score;
        try {
            Map<String, Object> st = p.status();
            if ("workbuddy".equals(p.id())) {
                int healthy = num(st.get("healthy"));
                long credits = 0;
                boolean anyCredits = false;
                Object accounts = st.get("accounts");
                if (accounts instanceof List) {
                    for (Object o : (List<?>) accounts) {
                        if (!(o instanceof Map)) continue;
                        Object cr = ((Map<?, ?>) o).get("credits");
                        if (cr instanceof Number) {
                            anyCredits = true;
                            credits += ((Number) cr).longValue();
                        }
                    }
                }
                if (healthy <= 0) score = -1;
                else if (anyCredits && credits <= 0) score = 800; // 有号但余额耗尽
                else score = 1000 + Math.min(credits, 500);
            } else if (p.id().startsWith("qoder")) {
                score = Boolean.TRUE.equals(st.get("signedIn")) ? 900 : -1;
            } else {
                score = 100;
            }
        } catch (Exception e) {
            score = -1;
        }
        scoreCache.put(p.id(), new long[]{score, now});
        return score;
    }

    private static int num(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    /** 跨 provider 命中模型（用于校验用户选的模型是否存在） */
    public String match(String requested) {
        if (requested == null || requested.isEmpty()) return null;
        for (UpstreamProvider p : providers) {
            String hit = p.catalog().match(requested);
            if (hit != null) return hit;
        }
        return null;
    }

    /** 合并去重后的模型 id 列表（页面模型选择用） */
    public List<String> allModelIds() {
        return publicCatalog().ids();
    }

    public String defaultModel() {
        for (UpstreamProvider p : providers) {
            String d = p.defaultModel();
            if (d != null && !d.isEmpty()) {
                String display = publicCatalog().match(d);
                return display == null ? d : display;
            }
        }
        return "";
    }

    /** 各 provider 的诊断信息，key=provider id */
    public Map<String, Object> statusMap() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (UpstreamProvider p : providers) {
            try {
                out.put(p.id(), p.status());
            } catch (Exception e) {
                out.put(p.id(), java.util.Collections.singletonMap("error", e.getMessage()));
            }
        }
        return out;
    }
}
