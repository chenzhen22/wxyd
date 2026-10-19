package com.cyz.wbgateway;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkBuddy 模型目录（/v3/config 实时拉取，失败时退回内置兜底目录）。
 * 只保留网关关心的字段：id/名称/免费标记（billing.free）。
 */
@Data
public class WbModelCatalog {

    public List<ModelInfo> models = new ArrayList<>();
    /** 目录来源：live=实时拉取 fallback=内置兜底 */
    public String source;

    public static class ModelInfo {
        public String id;
        public String name;
        public boolean free;
        public long maxTokens;
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (ModelInfo m : models) ids.add(m.id);
        return ids;
    }

    /** 免费模型 id 列表（billing.free，按目录顺序）——模型开关关闭时的静默映射目标 */
    public List<String> freeIds() {
        List<String> ids = new ArrayList<>();
        for (ModelInfo m : models) if (m.free) ids.add(m.id);
        return ids;
    }

    /** 精确或大小写不敏感命中目录，返回规范 id；未命中返回 null */
    public String match(String requested) {
        if (requested == null || requested.isEmpty()) return null;
        for (ModelInfo m : models) if (m.id.equals(requested)) return m.id;
        String lower = requested.toLowerCase();
        for (ModelInfo m : models) if (m.id.toLowerCase().equals(lower)) return m.id;
        return null;
    }

    public ModelInfo byId(String id) {
        if (id == null) return null;
        for (ModelInfo m : models) if (m.id.equals(id)) return m;
        return null;
    }

    /** 解析 /v3/config 信封 data（照插件 parseModelCatalog：cli agent 的 roster 交集中于可用行） */
    public static WbModelCatalog parse(JSONObject data) {
        WbModelCatalog catalog = new WbModelCatalog();
        JSONArray rawModels = data == null ? null : data.getJSONArray("models");
        JSONArray agents = data == null ? null : data.getJSONArray("agents");
        List<String> cliIds = new ArrayList<>();
        if (agents != null) {
            for (int i = 0; i < agents.size(); i++) {
                JSONObject agent = agents.getJSONObject(i);
                if (agent == null) continue;
                if ("cli".equals(agent.getString("name")) && agent.getJSONArray("models") != null) {
                    for (Object o : agent.getJSONArray("models")) {
                        if (o instanceof String) cliIds.add((String) o);
                    }
                    break;
                }
            }
        }
        if (cliIds.isEmpty()) {
            throw new IllegalStateException("model catalog lists no cli agent models");
        }
        java.util.Map<String, ModelInfo> byId = new java.util.HashMap<>();
        if (rawModels != null) {
            for (int i = 0; i < rawModels.size(); i++) {
                JSONObject row = rawModels.getJSONObject(i);
                if (row == null) continue;
                String id = row.getString("id");
                if (id == null || id.isEmpty() || Boolean.TRUE.equals(row.getBoolean("disabled"))) continue;
                Long input = row.getLong("maxInputTokens");
                Long output = row.getLong("maxOutputTokens");
                if (input == null || output == null || input <= 0 || output <= 0) continue;
                ModelInfo info = new ModelInfo();
                info.id = id;
                String name = row.getString("name");
                info.name = name == null || name.isEmpty() ? id : name;
                info.maxTokens = output;
                info.free = isFreeCredits(row.getString("credits"));
                byId.put(id, info);
            }
        }
        for (String id : cliIds) {
            ModelInfo info = byId.get(id);
            if (info != null) catalog.models.add(info);
        }
        if (catalog.models.isEmpty()) {
            throw new IllegalStateException("model catalog resolved to an empty list");
        }
        return catalog;
    }

    /** credits 形如 "x0.00 credits"；归一后匹配 x0.0* 视为免费（照插件 resolveUpstreamBilling） */
    private static boolean isFreeCredits(String credits) {
        if (credits == null || credits.trim().isEmpty()) return false;
        String bare = credits.trim();
        if (bare.toLowerCase().matches("^credits?$")) return false;
        bare = bare.replaceAll("(?i)\\s+credits?$", "").trim();
        return bare.matches("^x?0\\.0+$");
    }
}
