package com.chenzhen.mock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MockDataService {

    private static final String MOCK_BASE = "classpath:mock/";
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private String defaultJson;

    /**
     * 根据请求路径和参数匹配 mock JSON
     *
     * @param path   归一化后的请求路径，如 tbphx.do、doc/list
     * @param params 请求参数
     * @return JSON 字符串，未匹配返回 null
     */
    public String match(String path, Map<String, String> params) {
        // 1. 尝试按参数匹配: mock/{path}/{key=value}.json
        String paramMatch = tryParamMatch(path, params);
        if (paramMatch != null) {
            return paramMatch;
        }

        // 2. 尝试直接匹配文件: mock/{path}.json
        String directMatch = loadJson(path + ".json");
        if (directMatch != null) {
            return directMatch;
        }

        // 3. 兜底 default.json
        if (defaultJson == null) {
            defaultJson = loadJson("default.json");
        }
        return defaultJson;
    }

    private String tryParamMatch(String path, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        // 取第一个非空参数做匹配: mock/{path}/{key=value}.json
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StringUtils.hasText(entry.getValue())) {
                String resourcePath = path + "/" + entry.getKey() + "=" + entry.getValue() + ".json";
                String json = loadJson(resourcePath);
                if (json != null) {
                    return json;
                }
            }
        }
        return null;
    }

    private String loadJson(String resourcePath) {
        // 查缓存
        String cached = cache.get(resourcePath);
        if (cached != null) {
            return cached;
        }

        try {
            Resource resource = resolver.getResource(MOCK_BASE + resourcePath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    byte[] bytes = new byte[is.available()];
                    is.read(bytes);
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    cache.put(resourcePath, json);
                    log.debug("Loaded mock data: {}", resourcePath);
                    return json;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load mock data: {} - {}", resourcePath, e.getMessage());
        }
        return null;
    }
}
