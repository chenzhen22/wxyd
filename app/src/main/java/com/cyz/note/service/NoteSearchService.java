package com.cyz.note.service;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.mapper.mysqlMapper.NoteIndexMapper;
import com.cyz.note.model.Note;
import com.cyz.note.model.NoteIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 笔记检索服务（note_index 查询侧）。
 * <p>
 * 检索层收敛在本类，将来若要换成 ES/Lucene，只需替换实现，
 * 不影响 NoteController 与前端（R1 的「留口」要求）。
 * <p>
 * 🔒 权限与数据最小化策略（产品决策 Q1）：
 * 可见性由 SQL 强制（is_public=1 OR owner_id=当前用户）；
 * 正文命中仅对 content_indexed=1 的行生效 —— 他人 public 笔记正文不入库，
 * 因此只能按标题命中，这里把该事实如实反映为 contentSearchable=false。
 */
@Service
public class NoteSearchService {

    private static final Logger log = LoggerFactory.getLogger(NoteSearchService.class);

    /** 平台预置分类（R4：受控词表，防分类分裂） */
    private static final List<String> PRESET_CATEGORIES = Arrays.asList(
            "Java API", "Shell 脚本", "Dubbo", "SQL", "故障案例",
            "踩坑记录", "环境配置", "性能优化", "其他");

    private static final int DEFAULT_LIMIT = 100;
    private static final int SOURCE_NOTE_LIMIT = 50;
    private static final int MAX_TAGS_PER_NOTE = 10;
    private static final int MAX_TAG_LEN = 20;

    @Resource
    NoteIndexMapper noteIndexMapper;

    @Resource
    MysqlMapper mysqlMapper;

    /**
     * 全库检索。scope=mine|public|all，field=all|title|content。
     * keyword 为空时按可见性列出全部（相当于浏览）。
     */
    public List<Note> search(long currentUserId, String keyword, String scope, String field) {
        String kw = trimToNull(keyword);
        String sc = normalizeScope(scope);
        String fd = normalizeField(field);
        int limit = DEFAULT_LIMIT;
        List<NoteIndex> rows;
        try {
            rows = noteIndexMapper.search(currentUserId, kw, sc, fd, limit);
        } catch (Exception e) {
            log.warn("笔记索引检索失败，降级为目录扫描：{}", e.getMessage());
            return null; // 交给调用方走文件扫描兜底
        }
        return toNotes(currentUserId, rows, kw);
    }

    /** 索引行 → Note，计算命中标记与 contentSearchable */
    private List<Note> toNotes(long currentUserId, List<NoteIndex> rows, String keyword) {
        List<Note> list = new ArrayList<>();
        if (rows == null) return list;
        for (NoteIndex r : rows) {
            boolean mine = r.getOwnerId() != null && r.getOwnerId() == currentUserId;
            boolean titleHit = keyword != null
                    && r.getTitle() != null
                    && r.getTitle().toLowerCase().contains(keyword.toLowerCase());
            boolean contentIndexed = r.getContentIndexed() != null && r.getContentIndexed() == 1;
            boolean contentHit = keyword != null && contentIndexed
                    && r.getContent() != null
                    && r.getContent().toLowerCase().contains(keyword.toLowerCase());

            Note n = new Note();
            n.setName(r.getNoteName());
            n.setCategory(r.getCategory());
            n.setVisibility(r.getIsPublic() != null && r.getIsPublic() == 1 ? "public" : "private");
            n.setOwnerId(r.getOwnerId());
            n.setOwnerName(r.getOwnerName());
            n.setUpdateTime(r.getUpdateTime());
            n.setPreview(r.getPreview());
            n.setSourceKey(r.getSourceKey());
            n.setTags(splitTags(r.getTags()));
            // 命中标记：让前端能高亮，并解释「为什么搜不到正文」
            n.setTitleMatched(titleHit);
            n.setContentMatched(contentHit);
            // Q1：只有属主自己的笔记才入库正文，他人一律 false
            n.setContentSearchable(mine && contentIndexed);
            list.add(n);
        }
        return list;
    }

    /**
     * 资料页反查：哪些公开笔记引用了这个条目。
     * 路径裁剪由服务层在入口处做白名单校验，此处只负责查。
     */
    public List<Note> listPublicNotesBySource(String sourceKey) {
        if (trimToNull(sourceKey) == null) return new ArrayList<>();
        List<NoteIndex> rows = noteIndexMapper.listPublicBySource(sourceKey, SOURCE_NOTE_LIMIT);
        return toNotes(-1L, rows, null); // viewerId=-1：视为非属主，contentSearchable 恒 false
    }

    /**
     * 分类联想：平台预置词表 + 该用户已有分类（预置在前）。
     * 用于 R4「分类不再分裂」——前端不再自由输入已有分类。
     */
    public Map<String, Object> listCategories(long ownerId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> custom = safeList(noteIndexMapper.listCategories(ownerId));
        Set<String> present = new LinkedHashSet<>();
        List<Map<String, Object>> preset = new ArrayList<>();
        for (String c : PRESET_CATEGORIES) {
            present.add(c);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c);
            m.put("cnt", countOf(custom, c));
            m.put("preset", true);
            preset.add(m);
        }
        result.put("preset", preset);
        result.put("custom", custom);
        return result;
    }

    private int countOf(List<Map<String, Object>> rows, String name) {
        for (Map<String, Object> r : rows) {
            Object v = r.get("name");
            if (v != null && name.equals(String.valueOf(v))) {
                Object c = r.get("cnt");
                if (c instanceof Number) return ((Number) c).intValue();
            }
        }
        return 0;
    }

    /**
     * 标签聚合（逗号分隔字段在内存里拆分统计）。
     * 返回 [{name, cnt}]，按次数倒序。
     */
    public List<Map<String, Object>> listTags(long ownerId) {
        List<Map<String, Object>> raw = safeList(noteIndexMapper.listTags(ownerId));
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (Map<String, Object> r : raw) {
            Object v = r.get("name");
            if (v == null) continue;
            for (String t : splitTags(String.valueOf(v))) {
                counter.merge(t, 1, Integer::sum);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counter.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("cnt", e.getValue());
            out.add(m);
        }
        out.sort((a, b) -> {
            int d = ((Number) b.get("cnt")).intValue() - ((Number) a.get("cnt")).intValue();
            return d != 0 ? d : String.valueOf(a.get("name")).compareTo(String.valueOf(b.get("name")));
        });
        return out;
    }

    /**
     * 🔒 检索越权自检（Q1 验收）。
     * <p>
     * 做法：从「他人 public 笔记」的正文里取样一个真实词，再用本用户身份
     * 执行一次 field=content 检索 —— 结果必须为 0。
     * 这比「查库里有没有别人的正文」更贴近真实风险：public 笔记正文必须入库
     * （否则属主本人无法检索自己的笔记），真正的防线在查询条件而非数据缺失。
     *
     * @return 自检结果，pass=true 表示未发现越权
     */
    public Map<String, Object> auditForeignContent(long currentUserId) {
        Map<String, Object> data = new LinkedHashMap<>();
        String sample = sampleForeignContentWord(currentUserId);
        data.put("sampleWord", sample);
        if (sample == null) {
            // 没有他人 public 笔记可测，视为无风险（不是"通过"，是没样本）
            data.put("leakHits", 0);
            data.put("pass", true);
            data.put("note", "无他人公开笔记样本，跳过越权自检");
            data.put("publicWithContent", safeInt(countPublicWithContent()));
            return data;
        }
        List<NoteIndex> hits = noteIndexMapper.search(currentUserId, sample, null, "content", 100);
        int leak = hits == null ? 0 : hits.size();
        data.put("leakHits", leak);
        data.put("pass", leak == 0);
        data.put("publicWithContent", safeInt(countPublicWithContent()));
        data.put("note", leak == 0
                ? "以他人公开笔记正文取样检索，零命中，符合预期"
                : "发现越权：他人公开笔记正文被命中，需检查 search 的 owner_id 限定");
        return data;
    }

    /** 从他人 public 笔记正文里取一个样本词（取较长的中英文串，避免搜不准） */
    private String sampleForeignContentWord(long currentUserId) {
        try {
            List<NoteIndex> rows = noteIndexMapper.search(currentUserId, null, "public", null, 20);
            if (rows == null) return null;
            for (NoteIndex r : rows) {
                String c = r.getContent();
                if (c == null || c.trim().isEmpty()) continue;
                // 取正文里第一个较长的连续 ASCII 词，命中判定更确定
                java.util.regex.Matcher m =
                        java.util.regex.Pattern.compile("[A-Za-z][A-Za-z0-9_]{4,}").matcher(c);
                if (m.find()) return m.group();
                String plain = c.replaceAll("[^\\u4e00-\\u9fa5]", " ").trim();
                if (plain.length() >= 4) return plain.substring(0, 4);
            }
        } catch (Exception e) {
            log.warn("取样他人公开笔记正文失败：{}", e.getMessage());
        }
        return null;
    }

    private int countPublicWithContent() {
        Integer c = noteIndexMapper.countPublicWithContent();
        return c == null ? 0 : c;
    }

    private static int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    /* ---------------- 供 NoteDataService 复用的工具 ---------------- */

    /**
     * 标签规范化：去空白、去重（保持顺序，大小写不敏感去重）、限长限量。
     * 空输入返回 null（表示无标签），避免 frontmatter 写入空数组。
     */
    public static List<String> normalizeTags(List<String> raw) {
        if (raw == null || raw.isEmpty()) return null;
        Set<String> seen = new LinkedHashSet<>();
        for (String t : raw) {
            if (t == null) continue;
            String s = t.trim();
            if (s.isEmpty()) continue;
            if (s.length() > MAX_TAG_LEN) s = s.substring(0, MAX_TAG_LEN);
            boolean dup = false;
            for (String x : seen) {
                if (x.equalsIgnoreCase(s)) { dup = true; break; }
            }
            if (!dup) seen.add(s);
            if (seen.size() >= MAX_TAGS_PER_NOTE) break;
        }
        return seen.isEmpty() ? null : new ArrayList<>(seen);
    }

    /** 逗号分隔 → List；空串返回空列表 */
    public static List<String> splitTags(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null) return out;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** List → 逗号分隔存储；空返回 "" */
    public static String joinTags(List<String> tags) {
        return tags == null || tags.isEmpty() ? "" : String.join(",", tags);
    }

    static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeScope(String scope) {
        if (scope == null) return null;
        String s = scope.trim().toLowerCase();
        if ("mine".equals(s) || "public".equals(s)) return s;
        return null; // 非法值退化为「全部可见」，不报错也不放宽权限
    }

    private static String normalizeField(String field) {
        if (field == null) return null;
        String f = field.trim().toLowerCase();
        if ("title".equals(f) || "content".equals(f)) return f;
        return null; // 默认 all
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    /** 平台预置分类（供 R2 摘录时选默认分类） */
    public static List<String> presetCategories() {
        return PRESET_CATEGORIES;
    }

    /** 已用到的预置分类集合（判断某分类是否属于词表） */
    public static Set<String> presetCategorySet() {
        return new TreeSet<>(PRESET_CATEGORIES);
    }
}
