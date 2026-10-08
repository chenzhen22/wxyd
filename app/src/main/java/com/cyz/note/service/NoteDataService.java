package com.cyz.note.service;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.mapper.mysqlMapper.NoteIndexMapper;
import com.cyz.note.model.Note;
import com.cyz.note.model.NoteIndex;
import com.cyz.pojo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆笔记数据服务（用户级）。
 * <p>
 * 每个用户的笔记为 {@code wxyd.share.dir}/{userId}/ 下的 .md 文件，
 * 元数据内嵌 YAML frontmatter（name/category/visibility/tags/source），正文为完整 markdown。
 * visibility 缺省按 public 处理（兼容无该字段的旧笔记）。
 * <p>
 * v2：文件仍是唯一真相源，note_index 仅作检索加速，在
 * saveNote / deleteNote / migrateOld / rebuildIndex 中同步维护。
 */
@Service
public class NoteDataService {

    private static final Logger log = LoggerFactory.getLogger(NoteDataService.class);
    private static final String CLASSPATH_PATTERN = "classpath:data-note/*.md";
    /** 摘录来源白名单：只允许这两个 classpath 目录，禁止任意路径读取 */
    private static final String[] SOURCE_PREFIXES = {"javaapi/", "shell/", "dubbo/"};
    private static final long MAX_CONTENT_SIZE = 500L * 1024;
    private static final int MAX_TITLE_LEN = 50;

    @Value("${wxyd.share.dir:/apps/shareFile}")
    private String shareDir;

    // 注意：javax.annotation.Resource 与 org.springframework.core.io.Resource 同名，
    // 这里必须用全限定名，不能 import（否则与上面的 spring Resource 冲突）
    @javax.annotation.Resource
    MysqlMapper mysqlMapper;

    @javax.annotation.Resource
    NoteIndexMapper noteIndexMapper;

    @javax.annotation.Resource
    NoteSearchService noteSearchService;

    private File userDir(long userId) {
        File dir = new File(shareDir, String.valueOf(userId));
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("笔记目录创建失败：{}", dir.getAbsolutePath());
        }
        return dir;
    }

    /** 标题净化：去路径分隔符/控制字符、限长；非法返回 null */
    static String sanitizeTitle(String title) {
        if (title == null) return null;
        String t = title.trim().replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "");
        if (t.isEmpty() || t.startsWith(".")) return null;
        return t.length() > MAX_TITLE_LEN ? t.substring(0, MAX_TITLE_LEN) : t;
    }

    /** 解析 md：frontmatter(name/category/visibility/tags/source) + 正文 */
    static Note parseNoteFile(String raw) {
        String frontMatter = "";
        String body = raw;
        if (raw.startsWith("---")) {
            int endIndex = raw.indexOf("---", 3);
            if (endIndex != -1) {
                frontMatter = raw.substring(3, endIndex).trim();
                body = raw.substring(endIndex + 3).trim();
            }
        }
        Yaml yaml = new Yaml();
        Map<String, Object> meta;
        try {
            meta = yaml.load(frontMatter);
        } catch (Exception e) {
            return null;
        }
        if (meta == null) return null;
        Object name = meta.get("name");
        if (name == null || String.valueOf(name).trim().isEmpty()) return null;
        Note note = new Note();
        note.setName(String.valueOf(name).trim());
        Object category = meta.get("category");
        note.setCategory(category == null ? "" : String.valueOf(category).trim());
        // 缺省 public，兼容旧笔记
        Object visibility = meta.get("visibility");
        String vis = visibility == null ? "public" : String.valueOf(visibility).trim();
        note.setVisibility("private".equals(vis) ? "private" : "public");
        // v2 新增字段，缺失时保持 null/空，老笔记零迁移
        Object tags = meta.get("tags");
        if (tags instanceof List) {
            List<String> list = new ArrayList<>();
            for (Object t : (List<?>) tags) {
                if (t != null) list.add(String.valueOf(t).trim());
            }
            note.setTags(list.isEmpty() ? null : list);
        } else if (tags != null) {
            List<String> list = NoteSearchService.splitTags(String.valueOf(tags));
            note.setTags(list.isEmpty() ? null : list);
        }
        Object source = meta.get("source");
        note.setSourceKey(source == null ? null : String.valueOf(source).trim());
        note.setContent(body);
        return note;
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

    private Note readNoteFile(File f) {
        try {
            Note n = parseNoteFile(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            if (n != null) {
                n.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(f.lastModified())));
            }
            return n;
        } catch (IOException e) {
            log.warn("读取笔记失败：{}", f.getAbsolutePath(), e);
            return null;
        }
    }

    /** 列表项：只留预览，不带全文（减小响应体） */
    private Note toListItem(Note n) {
        n.setPreview();
        n.setContent(null);
        return n;
    }

    private String displayName(long userId) {
        User u = mysqlMapper.queryUserById(userId);
        if (u == null) return String.valueOf(userId);
        return (u.getDisplayName() != null && !u.getDisplayName().isEmpty()) ? u.getDisplayName() : u.getUsername();
    }

    private void fillOwner(Note n, long ownerId) {
        n.setOwnerId(ownerId);
        n.setOwnerName(displayName(ownerId));
    }

    /** 我的笔记列表，按更新时间倒序 */
    public List<Note> listMyNotes(long userId) {
        List<Note> list = new ArrayList<>();
        File dir = userDir(userId);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".md"));
        if (files != null) {
            for (File f : files) {
                Note n = readNoteFile(f);
                if (n != null) {
                    fillOwner(n, userId);
                    list.add(toListItem(n));
                }
            }
        }
        list.sort(Comparator.comparing(Note::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    /** 读取单篇：自己的任意笔记，或他人的 public 笔记 */
    public Note getNote(long viewerId, long ownerId, String name) {
        String title = sanitizeTitle(name);
        if (title == null) return null;
        File f = resolveFile(ownerId, title);
        if (f == null || !f.isFile()) return null;
        Note n = readNoteFile(f);
        if (n == null) return null;
        if (viewerId != ownerId && "private".equals(n.getVisibility())) {
            return null; // 他人的私有笔记
        }
        fillOwner(n, ownerId);
        return n;
    }

    /**
     * 保存（新增或更新）自己的笔记；oldName 非空且与 name 不同视为改名。
     * tags 可为 null（不传即保持本次覆盖写入，v2 语义：前端总是带上全部标签）。
     */
    public String saveNote(long userId, String name, String category, String visibility,
                           String content, String oldName, List<String> tags, String sourceKey) {
        String title = sanitizeTitle(name);
        if (title == null) throw new IllegalArgumentException("标题非法");
        if (content == null || content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_SIZE) {
            throw new IllegalArgumentException("笔记正文为空或超过 500KB");
        }
        if (!"private".equals(visibility)) visibility = "public";
        String cat = normalizeCategory(category);
        String oldTitle = sanitizeTitle(oldName);
        boolean rename = oldTitle != null && !oldTitle.equals(title);
        List<String> tagList = NoteSearchService.normalizeTags(tags);
        String src = sanitizeSourceKey(sourceKey);
        try {
            File dir = userDir(userId).getCanonicalFile();
            File target = new File(dir, title + ".md").getCanonicalFile();
            if (!target.getPath().startsWith(dir.getPath() + File.separator)) {
                throw new IllegalArgumentException("标题非法");
            }
            // 未指定 oldName（纯新增语义）时，目标已存在视为冲突
            if (!rename && target.exists() && (oldName == null || oldName.trim().isEmpty())) {
                throw new IllegalStateException("同名笔记已存在");
            }
            // 组装 md：frontmatter 用 Yaml.dump 保证特殊字符安全
            Map<String, Object> meta = new HashMap<>();
            meta.put("name", title);
            meta.put("category", cat);
            meta.put("visibility", visibility);
            if (tagList != null) meta.put("tags", tagList);
            if (src != null) meta.put("source", src);
            String md = "---\n" + new Yaml().dump(meta) + "---\n\n" + content;
            Files.write(target.toPath(), md.getBytes(StandardCharsets.UTF_8));
            // 改名：删除旧文件
            if (rename) {
                File oldFile = new File(dir, oldTitle + ".md");
                if (oldFile.getCanonicalPath().startsWith(dir.getPath() + File.separator)) {
                    oldFile.delete();
                }
                // 改名的旧索引必须清掉，否则会留下指向已不存在文件的幽灵行
                deleteIndex(userId, oldTitle);
            }
            // 同步索引（正文只对属主入库 —— Q1 决策）
            syncIndex(userId, title, cat, visibility, content, tagList, src);
        } catch (IOException e) {
            log.error("保存笔记失败：{}", title, e);
            throw new IllegalStateException("保存失败");
        }
        return title;
    }

    /** 兼容旧调用签名（无 tags/source） */
    public String saveNote(long userId, String name, String category, String visibility,
                           String content, String oldName) {
        return saveNote(userId, name, category, visibility, content, oldName, null, null);
    }

    /**
     * 写入/更新一行索引。
     * 🔒 Q1：只有属主自己的笔记才写 content 并置 content_indexed=1。
     * 他人 public 笔记在 rebuildIndex 时会置 0 且 content 留空 —— 库里根本不留副本。
     */
    private void syncIndex(long userId, String title, String category, String visibility,
                           String content, List<String> tags, String sourceKey) {
        try {
            NoteIndex row = new NoteIndex();
            row.setOwnerId(userId);
            row.setNoteName(title);
            row.setTitle(title);
            row.setCategory(category == null ? "" : category);
            row.setTags(NoteSearchService.joinTags(tags));
            row.setSourceKey(sourceKey == null ? "" : sourceKey);
            row.setIsPublic("private".equals(visibility) ? 0 : 1);
            // Q1 决策：保存者就是属主，正文入库
            row.setContentIndexed(1);
            row.setContent(content);
            row.setPreview(buildPreview(content));
            row.setOwnerName(displayName(userId));
            noteIndexMapper.upsert(row);
        } catch (Exception e) {
            log.error("笔记索引同步失败（不影响文件保存）：{} / {}", userId, title, e);
        }
    }

    private void deleteIndex(long userId, String title) {
        try {
            noteIndexMapper.deleteByOwnerAndName(userId, title);
        } catch (Exception e) {
            log.error("笔记索引删除失败：{} / {}", userId, title, e);
        }
    }

    /** 列表预览：去标签后截 150 字（与 Note.setPreview 保持一致的口径） */
    private static String buildPreview(String content) {
        if (content == null) return "";
        String plain = content.replaceAll("<[^>]+>", "").trim();
        return plain.length() > 150 ? plain.substring(0, 150) + "..." : plain;
    }

    /** 分类规范化：trim + 去内部空白；预置词表按原样保留，不做大小写折叠（避免"Java"与"Java API"混淆） */
    private static String normalizeCategory(String category) {
        if (category == null) return "";
        return category.trim().replaceAll("\\s+", " ");
    }

    /**
     * 来源 key 白名单校验：仅允许 javaapi/、shell/、dubbo/ 前缀，
     * 且不允许路径穿越字符。防止任意 classpath 文件被读进笔记。
     */
    static String sanitizeSourceKey(String sourceKey) {
        if (sourceKey == null) return null;
        String s = sourceKey.trim();
        if (s.isEmpty()) return null;
        if (s.contains("..") || s.contains("\\") || s.contains(" ") || s.startsWith("/")) {
            throw new IllegalArgumentException("来源标识非法");
        }
        boolean ok = false;
        for (String p : SOURCE_PREFIXES) {
            if (s.startsWith(p)) { ok = true; break; }
        }
        if (!ok) throw new IllegalArgumentException("来源标识非法");
        return s;
    }

    /** 删除自己的笔记，返回是否删除成功（同时清索引） */
    public boolean deleteNote(long userId, String name) {
        String title = sanitizeTitle(name);
        if (title == null) return false;
        File f = resolveFile(userId, title);
        boolean ok = f != null && f.isFile() && f.delete();
        if (ok) deleteIndex(userId, title);
        return ok;
    }

    /**
     * 检索笔记（v2：走 note_index 索引，支持标题+正文）。
     * scope=all|mine|public，field=all|title|content。
     * keyword 为空时按可见性列出全部。
     * <p>
     * 🔒 可见性由 mapper SQL 强制：(is_public=1 OR owner_id=当前用户)。
     * 🔒 Q1：他人 public 笔记正文不入库，只能标题命中（contentSearchable=false）。
     */
    public List<Note> searchNotes(long currentUserId, String keyword, String scope, String field) {
        List<Note> indexed = noteSearchService.search(currentUserId, keyword, scope, field);
        if (indexed != null) return indexed;
        // 索引不可用（表缺失/DB 异常）时降级为 v1 的目录扫描兜底，保证功能不中断
        log.warn("笔记检索降级为目录扫描（索引不可用）");
        return fallbackScan(currentUserId, keyword, scope);
    }

    /** 降级路径：遍历目录，仅按标题匹配（无正文检索能力） */
    private List<Note> fallbackScan(long currentUserId, String keyword, String scope) {
        List<Note> list = new ArrayList<>();
        String kw = NoteSearchService.trimToNull(keyword);
        String kwLower = kw == null ? null : kw.toLowerCase();
        boolean mineOnly = scope != null && "mine".equals(scope);
        boolean publicOnly = scope != null && "public".equals(scope);
        File root = new File(shareDir);
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) return list;
        for (File dir : dirs) {
            if (!dir.getName().matches("\\d+")) continue;
            long ownerId;
            try {
                ownerId = Long.parseLong(dir.getName());
            } catch (NumberFormatException e) {
                continue;
            }
            if (mineOnly && ownerId != currentUserId) continue;
            File[] files = dir.listFiles((d, name) -> name.endsWith(".md"));
            if (files == null) continue;
            for (File f : files) {
                Note n = readNoteFile(f);
                if (n == null) continue;
                boolean mine = ownerId == currentUserId;
                if (!mine && !"public".equals(n.getVisibility())) continue; // 私有笔记对他人不可见
                if (publicOnly && mine) continue;
                if (kwLower != null && !n.getName().toLowerCase().contains(kwLower)) continue;
                fillOwner(n, ownerId);
                n.setTitleMatched(kwLower != null && n.getName().toLowerCase().contains(kwLower));
                n.setContentMatched(false);
                n.setContentSearchable(mine);
                list.add(toListItem(n));
            }
        }
        list.sort(Comparator.comparing(Note::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    /**
     * 一键沉淀（R2）：把资料页条目存成自己的私有笔记。
     * <p>
     * 标题 = {来源标题}（摘录），重复则追加序号；visibility 默认 private（Q2 决策）；
     * source 写入 frontmatter 供正向跳转；category 取来源分类。
     * <p>
     * 🔒 sourceKey 走白名单校验（仅 javaapi/、shell/、dubbo/），禁止任意 classpath 读取。
     * dubbo/ 为可复用调用样例，由前端组装内容传入。
     *
     * @return 新建笔记的标题
     */
    public String saveFromSource(long userId, String sourceKey, String sourceTitle,
                                 String sourceCategory, String content) {
        String src = sanitizeSourceKey(sourceKey);
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("摘录内容为空");
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_SIZE) {
            throw new IllegalArgumentException("摘录内容超过 500KB");
        }
        String base = sanitizeTitle((sourceTitle == null || sourceTitle.trim().isEmpty()
                ? "摘录" : sourceTitle.trim()) + "（摘录）");
        if (base == null) throw new IllegalArgumentException("来源标题非法");
        String title = base;
        int seq = 2;
        while (resolveFile(userId, title) != null && resolveFile(userId, title).isFile()) {
            String suffix = " " + seq++;
            int room = MAX_TITLE_LEN - suffix.length();
            String head = base.length() > room ? base.substring(0, Math.max(1, room)) : base;
            title = head + suffix;
        }
        // Q2 决策：摘录默认私有（沉淀是个人行为，公开需用户主动切换）
        return saveNote(userId, title,
                sourceCategory == null ? "" : sourceCategory,
                "private", content, title, null, src);
    }

    /** 资料页反查引用该条目的公开笔记（双向关联的反向） */
    public List<Note> listPublicNotesBySource(String sourceKey) {
        try {
            return noteSearchService.listPublicNotesBySource(sanitizeSourceKey(sourceKey));
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }
    }

    /**
     * 全量重建索引（幂等，超管专用）。
     * 遍历所有用户目录重新解析 md 并写索引；正文只对属主入库（Q1）。
     *
     * @return 重建后的索引行数
     */
    public int rebuildIndex() {
        int total = 0;
        try {
            noteIndexMapper.truncateAll();
        } catch (Exception e) {
            log.error("清空笔记索引失败：{}", e.getMessage());
            throw new IllegalStateException("索引重建失败");
        }
        File root = new File(shareDir);
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) return 0;
        for (File dir : dirs) {
            if (!dir.getName().matches("\\d+")) continue;
            long ownerId;
            try {
                ownerId = Long.parseLong(dir.getName());
            } catch (NumberFormatException e) {
                continue;
            }
            File[] files = dir.listFiles((d, name) -> name.endsWith(".md"));
            if (files == null) continue;
            for (File f : files) {
                Note n = readNoteFile(f);
                if (n == null) continue;
                try {
                    NoteIndex row = new NoteIndex();
                    row.setOwnerId(ownerId);
                    row.setNoteName(sanitizeTitle(n.getName()));
                    row.setTitle(n.getName());
                    row.setCategory(n.getCategory() == null ? "" : n.getCategory());
                    row.setTags(NoteSearchService.joinTags(n.getTags()));
                    row.setSourceKey(n.getSourceKey() == null ? "" : n.getSourceKey());
                    row.setIsPublic("private".equals(n.getVisibility()) ? 0 : 1);
                    // Q1 决策：重建时同样只对属主入库正文
                    row.setContentIndexed(1);
                    row.setContent(n.getContent());
                    row.setPreview(buildPreview(n.getContent()));
                    row.setOwnerName(displayName(ownerId));
                    noteIndexMapper.upsert(row);
                    total++;
                } catch (Exception e) {
                    log.error("重建索引失败：{} / {}", ownerId, f.getName(), e);
                }
            }
        }
        log.info("笔记索引重建完成：共 {} 篇", total);
        return total;
    }

    /** 旧笔记迁移：classpath data-note/*.md 拷入目标用户目录，同名跳过；返回拷贝篇数 */
    public int migrateOld(long targetUserId) {
        File dir = userDir(targetUserId);
        int copied = 0;
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Note n = parseNoteFile(readInputStream(is));
                    if (n == null) continue;
                    File target = new File(dir, sanitizeTitle(n.getName()) + ".md");
                    if (target.exists()) continue; // 幂等：同名跳过
                    // 补 visibility/tags/source 字段后落盘
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("name", n.getName());
                    meta.put("category", n.getCategory());
                    meta.put("visibility", n.getVisibility());
                    if (n.getTags() != null) meta.put("tags", n.getTags());
                    if (n.getSourceKey() != null) meta.put("source", n.getSourceKey());
                    String md = "---\n" + new Yaml().dump(meta) + "---\n\n" + n.getContent();
                    Files.write(target.toPath(), md.getBytes(StandardCharsets.UTF_8));
                    // 迁移的笔记也进索引，否则重建前搜不到
                    syncIndex(targetUserId, sanitizeTitle(n.getName()), n.getCategory(),
                            n.getVisibility(), n.getContent(), n.getTags(), n.getSourceKey());
                    copied++;
                } catch (IOException e) {
                    log.warn("迁移旧笔记失败: {}", resource.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.warn("旧笔记扫描失败", e);
        }
        log.info("旧笔记迁移完成：目标用户 {}，拷贝 {} 篇", targetUserId, copied);
        return copied;
    }

    /** 目录内文件定位（防穿越：规范路径必须位于用户目录内） */
    private File resolveFile(long userId, String title) {
        try {
            File dir = userDir(userId).getCanonicalFile();
            File target = new File(dir, title + ".md").getCanonicalFile();
            if (!target.getPath().startsWith(dir.getPath() + File.separator)) {
                return null;
            }
            return target;
        } catch (IOException e) {
            return null;
        }
    }
}
