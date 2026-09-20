package com.cyz.shellscript.service;

import com.cyz.shellscript.model.ShellExample;
import com.cyz.shellscript.model.ShellSyntax;
import com.cyz.shellscript.model.ShellTopic;
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

/**
 * Shell 脚本学习数据服务。
 * <p>
 * 从 classpath:data-shell/*.md 加载只读学习内容，解析逻辑参考
 * {@code com.cyz.javaapi.service.ApiDataService}，但语法段保留代码块原文作为签名，
 * 示例段保留完整 bash 代码块（不含方法体标记），仅做展示、不执行。
 */
@Service
public class ShellScriptDataService {

    private static final Logger log = LoggerFactory.getLogger(ShellScriptDataService.class);
    private static final String CLASSPATH_PATTERN = "classpath:data-shell/*.md";

    private final Map<String, ShellTopic> topicMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        loadTopics();
    }

    /**
     * 重新加载所有 Shell 脚本学习数据（从 classpath 资源中读取，支持 jar 内嵌资源）
     */
    public synchronized int reload() {
        topicMap.clear();
        loadFromClasspath();
        int loaded = topicMap.size();
        log.info("Shell 脚本学习数据重新加载完成: {} 个主题已加载", loaded);
        return loaded;
    }

    private void loadTopics() {
        loadFromClasspath();
    }

    private void loadFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);
            log.info("发现 {} 个 Shell 脚本学习数据文件", resources.length);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    String content = readInputStream(is);
                    ShellTopic topic = parseTopicFile(content);
                    if (topic != null) {
                        topicMap.put(topic.getName(), topic);
                    }
                } catch (IOException e) {
                    log.warn("加载 Shell 脚本数据文件失败: {}", resource.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Classpath Shell 数据扫描失败", e);
        }
        log.info("Shell 脚本学习数据加载完成: {} 个主题", topicMap.size());
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

    private ShellTopic parseTopicFile(String content) {
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
        String category = metadata != null ? (String) metadata.get("category") : "";

        if (name == null || name.isEmpty()) return null;

        return parseDocument(name, category, body);
    }

    private ShellTopic parseDocument(String name, String category, String rawBody) {
        String intro = "";
        List<ShellSyntax> syntaxes = new ArrayList<>();
        List<ShellExample> examples = new ArrayList<>();

        String[] sections = rawBody.split("(?m)^## ");
        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            String[] lines = section.split("\n", 2);
            String sectionTitle = lines[0].trim();
            String sectionBody = lines.length > 1 ? lines[1].trim() : "";

            if (sectionTitle.equals("介绍")) {
                intro = parseIntroduction(sectionBody);
            } else if (sectionTitle.equals("语法")) {
                syntaxes = parseSyntaxes(sectionBody);
            } else if (sectionTitle.equals("示例")) {
                examples = parseExamples(sectionBody);
            }
        }

        return new ShellTopic(name, category, intro, syntaxes, examples);
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

    /**
     * 解析 ## 语法 段落：按 ### 切分，每节取首个 ```bash 代码块作为签名，
     * 其余以「- 描述」开头的行汇总为描述。
     */
    private List<ShellSyntax> parseSyntaxes(String body) {
        List<ShellSyntax> syntaxes = new ArrayList<>();
        String[] subSections = body.split("(?m)^### ");
        for (String section : subSections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            String[] lines = section.split("\n", 2);
            String syntaxName = lines[0].trim();
            String rest = lines.length > 1 ? lines[1].trim() : "";

            String signature = "";
            StringBuilder description = new StringBuilder();
            boolean inCodeBlock = false;

            for (String line : rest.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("```bash") || trimmed.startsWith("```sh") || trimmed.startsWith("```shell")) {
                    inCodeBlock = true;
                    continue;
                }
                if (inCodeBlock && trimmed.startsWith("```")) {
                    inCodeBlock = false;
                    continue;
                }
                if (inCodeBlock) {
                    if (signature.isEmpty()) {
                        signature = line.trim();
                    }
                } else if (trimmed.startsWith("- 描述") || trimmed.startsWith("- 描述:")) {
                    String descText = trimmed.replaceAll("- 描述:?\\s*", "");
                    if (description.length() > 0) description.append(" ");
                    description.append(descText);
                }
            }

            syntaxes.add(new ShellSyntax(syntaxName, signature, description.toString().trim()));
        }
        return syntaxes;
    }

    /**
     * 解析 ## 示例 段落：按 ### 切分，每节取「- 描述」作为描述，
     * 取 ```bash 代码块完整内容（不含围栏标记）作为代码。
     */
    private List<ShellExample> parseExamples(String body) {
        List<ShellExample> examples = new ArrayList<>();
        String[] subSections = body.split("(?m)^### ");
        for (String section : subSections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            String[] lines = section.split("\n", 2);
            String exampleName = lines[0].trim();
            String rest = lines.length > 1 ? lines[1].trim() : "";

            String description = "";
            StringBuilder codeBuilder = new StringBuilder();
            boolean inCodeBlock = false;

            for (String line : rest.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("```bash") || trimmed.startsWith("```sh") || trimmed.startsWith("```shell")) {
                    inCodeBlock = true;
                    continue;
                }
                if (inCodeBlock && trimmed.startsWith("```")) {
                    inCodeBlock = false;
                    continue;
                }
                if (inCodeBlock) {
                    codeBuilder.append(line).append("\n");
                }
                if (trimmed.startsWith("- 描述") || trimmed.startsWith("- 描述:")) {
                    description = trimmed.replaceAll("- 描述:?\\s*", "");
                }
            }

            String code = codeBuilder.toString().trim();
            if (!exampleName.isEmpty()) {
                examples.add(new ShellExample(exampleName, description, code));
            }
        }
        return examples;
    }

    /**
     * 列表视图：返回按 name 排序、intro 截断的主题摘要，不含语法/示例详情。
     */
    public List<ShellTopic> getTopics() {
        return topicMap.values().stream()
                .sorted(Comparator.comparing(ShellTopic::getName))
                .map(t -> new ShellTopic(t.getName(), t.getCategory(),
                        truncateIntro(t.getIntro()), Collections.emptyList(), Collections.emptyList()))
                .collect(Collectors.toList());
    }

    public ShellTopic getTopic(String name) {
        return topicMap.get(name);
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
