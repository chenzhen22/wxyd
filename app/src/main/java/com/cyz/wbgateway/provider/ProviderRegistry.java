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

    /** 跨 provider 命中模型（用于校验用户选的模型是否存在） */
    public String match(String requested) {
        if (requested == null || requested.isEmpty()) return null;
        for (UpstreamProvider p : providers) {
            String hit = p.catalog().match(requested);
            if (hit != null) return hit;
        }
        return null;
    }

    /** 合并后的模型 id 列表（管理页/模型选择用） */
    public List<String> allModelIds() {
        List<String> out = new ArrayList<>();
        for (UpstreamProvider p : providers) {
            for (String id : p.catalog().ids()) if (!out.contains(id)) out.add(id);
        }
        return out;
    }

    public String defaultModel() {
        for (UpstreamProvider p : providers) {
            String d = p.defaultModel();
            if (d != null && !d.isEmpty()) return d;
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
