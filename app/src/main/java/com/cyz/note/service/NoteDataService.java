package com.cyz.note.service;

import com.cyz.note.model.Note;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆笔记数据服务。
 * <p>
 * 从 classpath:data-note/*.md 加载只读笔记，解析逻辑参考
 * {@code com.cyz.shellscript.service.ShellScriptDataService}：frontmatter(name/category)
 * + 其余正文作为完整 markdown。与 Shell 脚本学习不同，本服务不拆分语法/示例段，
 * 笔记以整篇 markdown 展示。
 */
@Service
public class NoteDataService {

    private static final Logger log = LoggerFactory.getLogger(NoteDataService.class);
    private static final String CLASSPATH_PATTERN = "classpath:data-note/*.md";

    private final Map<String, Note> noteMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        loadNotes();
    }

    /**
     * 重新加载所有记忆笔记（从 classpath 资源中读取，支持 jar 内嵌资源）
     */
    public synchronized int reload() {
        noteMap.clear();
        loadFromClasspath();
        int loaded = noteMap.size();
        log.info("记忆笔记数据重新加载完成: {} 篇", loaded);
        return loaded;
    }

    private void loadNotes() {
        loadFromClasspath();
    }

    private void loadFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);
            log.info("发现 {} 个记忆笔记数据文件", resources.length);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    String content = readInputStream(is);
                    Note note = parseNoteFile(content);
                    if (note != null) {
                        noteMap.put(note.getName(), note);
                    }
                } catch (IOException e) {
                    log.warn("加载记忆笔记数据文件失败: {}", resource.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Classpath 笔记数据扫描失败", e);
        }
        log.info("记忆笔记数据加载完成: {} 篇", noteMap.size());
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

    private Note parseNoteFile(String content) {
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

        return new Note(name, category, body);
    }

    /**
     * 列表视图：按 category 再按 name 排序，content 截断为预览，不含完整正文。
     */
    public List<Note> getNotes() {
        return noteMap.values().stream()
                .sorted(Comparator.comparing(Note::getCategory, Comparator.nullsLast(String::compareTo))
                        .thenComparing(Note::getName, Comparator.nullsLast(String::compareTo)))
                .map(n -> new Note(n.getName(), n.getCategory(), truncateContent(n.getContent())))
                .collect(Collectors.toList());
    }

    public Note getNote(String name) {
        return noteMap.get(name);
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        String plain = content.replaceAll("<[^>]+>", "").trim();
        if (plain.length() > 150) {
            return plain.substring(0, 150) + "...";
        }
        return plain;
    }
}
