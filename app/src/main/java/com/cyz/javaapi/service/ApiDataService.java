package com.cyz.javaapi.service;

import com.cyz.javaapi.model.ApiClass;
import com.cyz.javaapi.model.ApiMethod;
import com.cyz.javaapi.model.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApiDataService {

    private static final Logger log = LoggerFactory.getLogger(ApiDataService.class);
    private static final String CLASSPATH_PATTERN = "classpath:data/*.md";

    private final Map<String, ApiClass> apiClassMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        loadApiClasses();
    }

    /**
     * 重新加载所有 API 数据（从 classpath 资源中读取，支持 jar 内嵌资源）
     */
    public synchronized int reload() {
        apiClassMap.clear();
        loadFromClasspath();
        int loaded = apiClassMap.size();
        log.info("Java API 重新加载完成: {} 个 API 已加载", loaded);
        return loaded;
    }

    private void loadApiClasses() {
        loadFromClasspath();
    }

    private void loadFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);
            log.info("发现 {} 个 Java API 数据文件", resources.length);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    String content = readInputStream(is);
                    ApiClass apiClass = parseApiFile(content);
                    if (apiClass != null) {
                        apiClassMap.put(apiClass.getName(), apiClass);
                    }
                } catch (IOException e) {
                    log.warn("加载 API 数据文件失败: {}", resource.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Classpath 数据扫描失败", e);
        }
        log.info("Java API 加载完成: {} 个 API", apiClassMap.size());
    }

    private static String readInputStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private ApiClass parseApiFile(String content) {
        String frontMatter = "";
        String body = content;

        if (content.startsWith("---")) {
            int endIndex = content.indexOf("---", 3);
            if (endIndex != -1) {
                frontMatter = content.substring(3, endIndex).trim();
                body = content.substring(endIndex + 3).trim();
            }
        }

        Yaml yaml = new Yaml();
        Map<String, Object> metadata = yaml.load(frontMatter);
        String name = metadata != null ? (String) metadata.get("name") : "";
        String packageName = metadata != null ? (String) metadata.get("package") : "";

        if (name == null || name.isEmpty()) return null;

        return parseDocument(name, packageName, body);
    }

    private ApiClass parseDocument(String name, String packageName, String rawBody) {
        String intro = "";
        List<ApiMethod> methods = new ArrayList<>();
        List<TestCase> testCases = new ArrayList<>();

        String[] sections = rawBody.split("(?m)^## ");
        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            String[] lines = section.split("\n", 2);
            String sectionTitle = lines[0].trim();
            String sectionBody = lines.length > 1 ? lines[1].trim() : "";

            if (sectionTitle.equals("介绍")) {
                intro = parseIntroduction(sectionBody);
            } else if (sectionTitle.equals("方法")) {
                methods = parseMethods(sectionBody);
            } else if (sectionTitle.equals("测试")) {
                testCases = parseTestCases(sectionBody);
            }
        }

        return new ApiClass(name, packageName, intro, methods, testCases);
    }

    private String parseIntroduction(String body) {
        StringBuilder sb = new StringBuilder();
        String[] lines = body.split("\n");
        boolean inCode = false;
        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            if (!inCode) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private List<ApiMethod> parseMethods(String body) {
        List<ApiMethod> methods = new ArrayList<>();
        String[] methodSections = body.split("(?m)^### ");
        for (String section : methodSections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            String[] lines = section.split("\n", 2);
            String methodName = lines[0].trim();
            String rest = lines.length > 1 ? lines[1].trim() : "";

            String signature = "";
            StringBuilder description = new StringBuilder();
            String returnType = "";
            List<String> params = new ArrayList<>();

            String[] subLines = rest.split("\n");
            boolean inCodeBlock = false;
            for (String line : subLines) {
                if (line.trim().startsWith("```java")) {
                    inCodeBlock = true;
                    continue;
                }
                if (inCodeBlock && line.trim().startsWith("```")) {
                    inCodeBlock = false;
                    continue;
                }
                if (inCodeBlock) {
                    signature = line.trim();
                    String sig = signature;
                    if (sig.contains(" ")) {
                        String[] parts = sig.split("\\s+");
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].contains("(")) {
                                break;
                            }
                            if (!parts[i].equals("public") && !parts[i].equals("static")
                                    && !parts[i].equals("<U>") && !parts[i].equals("<V>")
                                    && !parts[i].equals("final") && !parts[i].equals(" synchronized")) {
                                returnType = parts[i];
                            }
                        }
                    }
                } else if (!line.trim().startsWith("```")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("- **参数**") || trimmed.startsWith("- **参数**:")) {
                        String paramText = trimmed.replaceAll("- \\*\\*参数\\*\\*:?\\s*", "");
                        if (!paramText.isEmpty()) {
                            params.add(paramText);
                        }
                    } else if (!trimmed.startsWith("- **")) {
                        description.append(trimmed).append(" ");
                    }
                }
            }

            methods.add(new ApiMethod(methodName, signature, description.toString().trim(), returnType, params));
        }
        return methods;
    }

    private List<TestCase> parseTestCases(String body) {
        List<TestCase> testCases = new ArrayList<>();
        String[] testSections = body.split("(?m)^### ");
        for (String section : testSections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            String[] lines = section.split("\n", 2);
            String testName = lines[0].trim();
            String rest = lines.length > 1 ? lines[1].trim() : "";

            String description = "";
            String assertion = "";
            String code = "";
            boolean inCodeBlock = false;
            boolean inMethodBody = false;
            StringBuilder codeBuilder = new StringBuilder();

            for (String line : rest.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("```java")) {
                    inCodeBlock = true;
                    continue;
                }
                if (inCodeBlock && trimmed.startsWith("```")) {
                    inCodeBlock = false;
                    continue;
                }
                if (inCodeBlock) {
                    if (trimmed.contains("// 方法体开始")) {
                        inMethodBody = true;
                        continue;
                    }
                    if (trimmed.contains("// 方法体结束")) {
                        inMethodBody = false;
                        continue;
                    }
                    if (inMethodBody) {
                        codeBuilder.append(line).append("\n");
                    }
                }
                if (trimmed.startsWith("- 描述") || trimmed.startsWith("- 描述:")) {
                    description = trimmed.replaceAll("- 描述:?\\s*", "");
                }
                if (trimmed.startsWith("- 断言") || trimmed.startsWith("- 断言:")) {
                    assertion = trimmed.replaceAll("- 断言:?\\s*", "");
                }
            }

            code = codeBuilder.toString().trim();
            if (!testName.isEmpty() && !code.isEmpty()) {
                testCases.add(new TestCase(testName, description, assertion, code, ""));
            }
        }
        return testCases;
    }

    public List<ApiClass> getApiClasses() {
        return apiClassMap.values().stream()
                .sorted(Comparator.comparing(ApiClass::getName))
                .map(ac -> new ApiClass(ac.getName(), ac.getPackageName(),
                        truncateIntro(ac.getIntro()), Collections.emptyList(), Collections.emptyList()))
                .collect(Collectors.toList());
    }

    public ApiClass getApiClass(String name) {
        return apiClassMap.get(name);
    }

    private String truncateIntro(String intro) {
        if (intro == null) return "";
        String plain = intro.replaceAll("<[^>]+>", "").trim();
        if (plain.length() > 150) {
            return plain.substring(0, 150) + "...";
        }
        return plain;
    }
}
