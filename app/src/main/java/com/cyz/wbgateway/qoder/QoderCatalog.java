package com.cyz.wbgateway.qoder;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.wbgateway.provider.ModelCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Qoder 模型目录：id 统一加 {@code qoder:} 前缀（避免与 WorkBuddy 目录重名），
 * 上游真实模型 key 保存在 {@link ModelInfo#key}。
 */
public class QoderCatalog implements ModelCatalog {

    public static class ModelInfo {
        public String id;
        public String key;
        public String name;
        public boolean alwaysThinking;
        /** 目录标记为免费（is_free=true 或 price_factor=0） */
        public boolean free;
    }

    private final List<ModelInfo> models;

    private QoderCatalog(List<ModelInfo> models) {
        this.models = models;
    }

    public static QoderCatalog empty() {
        return new QoderCatalog(new ArrayList<>());
    }

    public static QoderCatalog parse(JSONArray chat, String prefix) {
        List<ModelInfo> list = new ArrayList<>();
        if (chat != null) {
            for (int i = 0; i < chat.size(); i++) {
                JSONObject row = chat.getJSONObject(i);
                if (row == null) continue;
                String key = row.getString("key");
                String name = row.getString("display_name");
                if (key == null || key.isEmpty() || name == null || name.isEmpty()) continue;
                if (Boolean.FALSE.equals(row.getBoolean("enable"))) continue;
                ModelInfo m = new ModelInfo();
                m.key = key;
                m.name = name;
                m.id = prefix + name.replaceAll("\\s+", "");
                JSONObject thinking = row.getJSONObject("thinking_config");
                m.alwaysThinking = thinking != null && !thinking.containsKey("disabled");
                boolean free = Boolean.TRUE.equals(row.getBoolean("is_free"));
                Object price = row.get("price_factor");
                if (!free && price instanceof Number && ((Number) price).doubleValue() == 0d) free = true;
                m.free = free;
                list.add(m);
            }
        }
        return new QoderCatalog(list);
    }

    public ModelInfo byId(String id) {
        if (id == null) return null;
        for (ModelInfo m : models) if (m.id.equals(id)) return m;
        return null;
    }

    public List<ModelInfo> getModels() {
        return models;
    }

    @Override
    public List<String> ids() {
        List<String> out = new ArrayList<>();
        for (ModelInfo m : models) out.add(m.id);
        return out;
    }

    /** 目录标记为免费的模型（模型开关关闭时的静默映射目标） */
    @Override
    public List<String> freeIds() {
        List<String> out = new ArrayList<>();
        for (ModelInfo m : models) if (m.free) out.add(m.id);
        return out;
    }

    @Override
    public String match(String requested) {
        if (requested == null || requested.isEmpty()) return null;
        for (ModelInfo m : models) if (m.id.equals(requested)) return m.id;
        String lower = requested.toLowerCase();
        for (ModelInfo m : models) if (m.id.toLowerCase().equals(lower)) return m.id;
        return null;
    }

    @Override
    public String source() {
        return "live";
    }
}
