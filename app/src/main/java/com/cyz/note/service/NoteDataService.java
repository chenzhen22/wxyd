package com.cyz.note.service;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.note.model.Note;
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
 * 元数据内嵌 YAML frontmatter（name/category/visibility），正文为完整 markdown。
 * visibility 缺省按 public 处理（兼容无该字段的旧笔记）。
 */
@Service
public class NoteDataService {

    private static final Logger log = LoggerFactory.getLogger(NoteDataService.class);
    private static final String CLASSPATH_PATTERN = "classpath:data-note/*.md";
    private static final long MAX_CONTENT_SIZE = 500L * 1024;
    private static final int MAX_TITLE_LEN = 50;

    @Value("${wxyd.share.dir:/apps/shareFile}")
    private String shareDir;

    @javax.annotation.Resource
    MysqlMapper mysqlMapper;

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

    /** 解析 md：frontmatter(name/category/visibility) + 正文 */
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

    /** 保存（新增或更新）自己的笔记；oldName 非空且与 name 不同视为改名 */
    public String saveNote(long userId, String name, String category, String visibility, String content, String oldName) {
        String title = sanitizeTitle(name);
        if (title == null) throw new IllegalArgumentException("标题非法");
        if (content == null || content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_SIZE) {
            throw new IllegalArgumentException("笔记正文为空或超过 500KB");
        }
        if (!"private".equals(visibility)) visibility = "public";
        String cat = category == null ? "" : category.trim();
        String oldTitle = sanitizeTitle(oldName);
        boolean rename = oldTitle != null && !oldTitle.equals(title);
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
            String md = "---\n" + new Yaml().dump(meta) + "---\n\n" + content;
            Files.write(target.toPath(), md.getBytes(StandardCharsets.UTF_8));
            // 改名：删除旧文件
            if (rename) {
                File oldFile = new File(dir, oldTitle + ".md");
                if (oldFile.getCanonicalPath().startsWith(dir.getPath() + File.separator)) {
                    oldFile.delete();
                }
            }
        } catch (IOException e) {
            log.error("保存笔记失败：{}", title, e);
            throw new IllegalStateException("保存失败");
        }
        return title;
    }

    /** 删除自己的笔记，返回是否删除成功 */
    public boolean deleteNote(long userId, String name) {
        String title = sanitizeTitle(name);
        if (title == null) return false;
        File f = resolveFile(userId, title);
        return f != null && f.isFile() && f.delete();
    }

    /** 按标题模糊搜索公共笔记（所有用户目录），带属主名。keyword 为空时返回全部公共笔记 */
    public List<Note> searchPublic(String keyword) {
        List<Note> list = new ArrayList<>();
        String kw = (keyword == null) ? null : keyword.trim().toLowerCase();
        boolean filterKw = kw != null && !kw.isEmpty();
        File root = new File(shareDir);
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) return list;
        for (File dir : dirs) {
            if (!dir.getName().matches("\\d+")) continue; // 仅用户 id 目录
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
                if (n == null || !"public".equals(n.getVisibility())) continue;
                if (filterKw && !n.getName().toLowerCase().contains(kw)) continue;
                fillOwner(n, ownerId);
                list.add(toListItem(n));
            }
        }
        list.sort(Comparator.comparing(Note::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
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
                    // 补 visibility 字段后落盘
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("name", n.getName());
                    meta.put("category", n.getCategory());
                    meta.put("visibility", n.getVisibility());
                    String md = "---\n" + new Yaml().dump(meta) + "---\n\n" + n.getContent();
                    Files.write(target.toPath(), md.getBytes(StandardCharsets.UTF_8));
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
