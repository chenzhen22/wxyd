package com.cyz.wbgateway.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 跨 provider 的合并模型目录：同名模型只展示一条，内部记住各 provider 的原生 id。
 * <p>
 * 展示 id 规则：有 WorkBuddy 同名模型时用其原生 id（兼容存量 Key），否则用去掉
 * provider 前缀的原生 id（如 qoder:Qwen3.8-Max → Qwen3.8-Max）。
 * 请求命中后由 {@link ProviderRegistry#resolve} 按账号可用性/余额选择实际 provider。
 */
public class CompositeCatalog implements ModelCatalog {

    public static class Group {
        public String display;
        public String norm;
        /** providerId -> 该 provider 的原生模型 id */
        public final Map<String, String> nativeByProvider = new LinkedHashMap<>();
        /** 可用于 match 的别名（原生 id 与小写形式） */
        public final Set<String> aliases = new LinkedHashSet<>();
    }

    private final List<Group> groups = new ArrayList<>();

    public static CompositeCatalog build(List<UpstreamProvider> providers, ModelCatalog defaults) {
        CompositeCatalog c = new CompositeCatalog();
        Map<String, Group> byNorm = new LinkedHashMap<>();
        for (UpstreamProvider p : providers) {
            ModelCatalog cat;
            try {
                cat = p.catalog();
            } catch (Exception e) {
                continue;
            }
            if (cat == null) continue;
            for (String nativeId : cat.ids()) {
                String norm = normalize(nativeId);
                Group g = byNorm.get(norm);
                if (g == null) {
                    g = new Group();
                    g.norm = norm;
                    g.display = displayOf(nativeId);
                    byNorm.put(norm, g);
                    c.groups.add(g);
                }
                // 同名时优先保留 WorkBuddy 的原生 id 作为展示（存量 Key 直接可用）
                if ("workbuddy".equals(p.id()) || g.nativeByProvider.isEmpty()) {
                    if ("workbuddy".equals(p.id())) g.display = nativeId;
                }
                g.nativeByProvider.put(p.id(), nativeId);
                g.aliases.add(nativeId);
                g.aliases.add(nativeId.toLowerCase());
                g.aliases.add(stripPrefix(nativeId).toLowerCase());
            }
        }
        return c;
    }

    /** 统一比较键：忽略大小写、空格、连字符、点等分隔符 */
    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String displayOf(String nativeId) {
        return stripPrefix(nativeId);
    }

    private static String stripPrefix(String nativeId) {
        int idx = nativeId.indexOf(':');
        return idx > 0 ? nativeId.substring(idx + 1) : nativeId;
    }

    public Group find(String requested) {
        if (requested == null || requested.isEmpty()) return null;
        String lower = requested.trim().toLowerCase();
        for (Group g : groups) {
            if (g.aliases.contains(lower) || g.display.equalsIgnoreCase(requested)) return g;
        }
        String norm = normalize(requested);
        for (Group g : groups) if (g.norm.equals(norm)) return g;
        return null;
    }

    public List<Group> getGroups() {
        return groups;
    }

    /** 某个 provider 的免费模型，映射成合并目录的展示 id */
    public List<String> freeIdsOf(UpstreamProvider provider) {
        List<String> out = new ArrayList<>();
        try {
            for (String nativeId : provider.catalog().freeIds()) {
                Group g = find(nativeId);
                if (g != null) out.add(g.display);
            }
        } catch (Exception ignore) {
        }
        return out;
    }

    /** 展示 id -> 指定 provider 的原生 id（该 provider 没有此模型返回 null） */
    public String nativeIdFor(String display, String providerId) {
        Group g = find(display);
        return g == null ? null : g.nativeByProvider.get(providerId);
    }

    @Override
    public List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Group g : groups) out.add(g.display);
        return out;
    }

    @Override
    public List<String> freeIds() {
        List<String> out = new ArrayList<>();
        for (Group g : groups) {
            for (String nativeId : g.nativeByProvider.values()) {
                if (nativeId.endsWith("Auto") || nativeId.endsWith("auto")) out.add(g.display);
            }
        }
        return out;
    }

    @Override
    public String match(String requested) {
        Group g = find(requested);
        return g == null ? null : g.display;
    }

    @Override
    public String source() {
        return "live";
    }
}
