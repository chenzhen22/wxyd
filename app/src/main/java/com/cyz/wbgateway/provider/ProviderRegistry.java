package com.cyz.wbgateway.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** provider 注册表：按模型 id 路由到对应适配器。 */
@Component
@Slf4j
public class ProviderRegistry {

    @Autowired(required = false)
    private List<UpstreamProvider> providers = new ArrayList<>();

    /** 合并目录（同名去重）缓存 60s；provider 自身的账号/额度信息各自缓存 10 分钟 */
    private volatile CompositeCatalog pubCatalog;
    private volatile long pubAt;

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
        long ttl = 60_000L;
        if (c != null) {
            // 有 provider 目录为空（还没拉到/临时失败）时缩短缓存，避免长时间缺模型
            for (UpstreamProvider p : providers) {
                try {
                    if (p.catalog().ids().isEmpty()) { ttl = 5_000L; break; }
                } catch (Exception ignore) {
                }
            }
        }
        if (c == null || now - pubAt > ttl) {
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
        List<UpstreamProvider> list = candidates(requested);
        if (list.isEmpty()) {
            UpstreamProvider p = providers.isEmpty() ? null : providers.get(0);
            return new Route(p, requested, requested);
        }
        UpstreamProvider best = list.get(0);
        if (g == null) return new Route(best, requested, requested);
        return new Route(best, g.nativeByProvider.get(best.id()), g.display);
    }

    /**
     * 同一模型的可选 provider，按优先级排序（无厂商特判）：
     * 1) 免费优先于计量；
     * 2) 都计量时，积分到期早的先消耗（未知到期视为最晚）；
     * 3) 同档位按可用账号数、剩余积分排序。
     * 模型不在任何目录（如静默映射）时，按 3) 的容量排序。
     */
    public List<UpstreamProvider> candidates(String display) {
        CompositeCatalog.Group g = publicCatalog().find(display);
        List<UpstreamProvider> list = new ArrayList<>();
        if (g != null) {
            for (UpstreamProvider p : providers) {
                if (g.nativeByProvider.containsKey(p.id())) list.add(p);
            }
        } else {
            // 合并目录里没有（可能某 provider 目录当时未就绪）：退回各 provider 原生目录匹配
            for (UpstreamProvider p : providers) {
                if (p.catalog().match(display) != null) list.add(p);
            }
            if (list.isEmpty()) list.addAll(providers);
        }
        // 不可用（无账号/余额明确为 0）的 provider 排到最后，仅作兜底
        List<UpstreamProvider> usable = new ArrayList<>();
        List<UpstreamProvider> unusable = new ArrayList<>();
        for (UpstreamProvider p : list) {
            if (p.usableAccountCount() > 0 && p.remainingCredits() != 0) usable.add(p);
            else unusable.add(p);
        }
        usable.sort((a, b) -> {
            boolean known = g != null;
            boolean fa = known && a.isFree(g.nativeByProvider.get(a.id()));
            boolean fb = known && b.isFree(g.nativeByProvider.get(b.id()));
            if (fa != fb) return fa ? -1 : 1;
            if (known && !fa && !fb) {
                // 同名计量模型：优先消耗积分先到期的
                long ea = a.creditsExpireAtMs();
                long eb = b.creditsExpireAtMs();
                if (ea != eb) return Long.compare(ea, eb);
            }
            if (!known) {
                // 静默映射：优先选有免费模型的 provider（否则会映射到付费的默认模型）
                boolean ha = !a.catalog().freeIds().isEmpty();
                boolean hb = !b.catalog().freeIds().isEmpty();
                if (ha != hb) return ha ? -1 : 1;
            }
            int c = Integer.compare(b.usableAccountCount(), a.usableAccountCount());
            if (c != 0) return c;
            return Long.compare(b.remainingCredits(), a.remainingCredits());
        });
        usable.addAll(unusable);
        return usable;
    }

    /** 跨 provider 命中模型（合并目录，支持带前缀或不带前缀的写法） */
    public String match(String requested) {
        if (requested == null || requested.isEmpty()) return null;
        return publicCatalog().match(requested);
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
