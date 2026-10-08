package com.cyz.note.controller;

import com.cyz.constant.ErrorEnum;
import com.cyz.note.model.Note;
import com.cyz.note.service.NoteDataService;
import com.cyz.note.service.NoteSearchService;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆笔记控制器（用户级，可编辑）—— 个人知识库 v2。
 * <p>
 * 路径前缀 note（wxyd context-path 为 /api，完整路径 /api/note/**），
 * 鉴权由 AuthInterceptor 的 /note/** 作用域保障（必须登录）。
 * 不实现 CommController，避免被 MockAspect 兜底拦截。
 * <p>
 * 🔒 权限红线：所有接口以 session 中的 userId 为准，
 * 私有笔记在任何搜索路径下都不返回；读他人私有笔记返回「不存在」而非「无权限」。
 */
@RestController
@RequestMapping("note")
@Slf4j
public class NoteController {

    @Autowired
    private NoteDataService noteDataService;

    @Autowired
    private NoteSearchService noteSearchService;

    /** 我的笔记列表 */
    @GetMapping("list")
    public Result list(HttpSession session) {
        Result result = Result.getInstance();
        List<Note> list = noteDataService.listMyNotes(userId(session));
        result.setBody(list);
        return result;
    }

    /**
     * 检索笔记（v2 升级）：支持标题 + 正文全文检索。
     *
     * @param keyword 关键词，空=浏览
     * @param scope   mine=我的笔记；public=他人公开；缺省=全部可见
     * @param field   all=标题+正文；title=仅标题；content=仅正文
     */
    @GetMapping("search")
    public Result search(@RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "scope", required = false) String scope,
                         @RequestParam(value = "field", required = false) String field,
                         HttpSession session) {
        Result result = Result.getInstance();
        long uid = userId(session);
        List<Note> list = noteDataService.searchNotes(uid, keyword, scope, field);
        result.setBody(list);
        return result;
    }

    /** 读单篇：自己的任意笔记，或他人的 public 笔记 */
    @GetMapping("get")
    public Result get(@RequestParam("ownerId") Long ownerId,
                      @RequestParam("name") String name,
                      HttpSession session) {
        Result result = Result.getInstance();
        if (ownerId == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        Note note = noteDataService.getNote(userId(session), ownerId, name);
        if (note == null) {
            // 他人私有笔记同样返回「不存在」，不泄露"该笔记存在"这一信息
            return result.setErrorEnum(ErrorEnum.ERROR000018);
        }
        result.setBody(note);
        return result;
    }

    /** 新增/更新自己的笔记 */
    @PostMapping("save")
    public Result save(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            String title = noteDataService.saveNote(
                    userId(session),
                    str(body.get("name")),
                    str(body.get("category")),
                    str(body.get("visibility")),
                    str(body.get("content")),
                    str(body.get("oldName")),
                    strList(body.get("tags")),
                    str(body.get("source")));
            result.setBody(title);
        } catch (IllegalArgumentException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        } catch (IllegalStateException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 删除自己的笔记 */
    @PostMapping("delete")
    public Result delete(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        boolean ok = noteDataService.deleteNote(userId(session), str(body.get("name")));
        if (!ok) {
            return result.setErrorEnum(ErrorEnum.ERROR000018);
        }
        return result;
    }

    /**
     * 一键沉淀（R2）：把资料页条目存成自己的私有笔记。
     * body: {sourceKey, sourceTitle, sourceCategory, content}
     * sourceKey 仅接受 javaapi/、shell/、dubbo/ 前缀（服务端白名单校验）。
     */
    @PostMapping("saveFromSource")
    public Result saveFromSource(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            String title = noteDataService.saveFromSource(
                    userId(session),
                    str(body.get("sourceKey")),
                    str(body.get("sourceTitle")),
                    str(body.get("sourceCategory")),
                    str(body.get("content")));
            Map<String, Object> data = new HashMap<>();
            data.put("name", title);
            result.setBody(data);
        } catch (IllegalArgumentException e) {
            result.setErrorEnum(ErrorEnum.ERROR000020);
            result.setErrorMsg(e.getMessage());
        } catch (IllegalStateException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    /** 分类联想（R4）：平台预置词表 + 我的已有分类 */
    @GetMapping("categories")
    public Result categories(HttpSession session) {
        Result result = Result.getInstance();
        result.setBody(noteSearchService.listCategories(userId(session)));
        return result;
    }

    /** 标签列表及使用次数（R5） */
    @GetMapping("tags")
    public Result tags(HttpSession session) {
        Result result = Result.getInstance();
        result.setBody(noteSearchService.listTags(userId(session)));
        return result;
    }

    /** 资料页反查：哪些公开笔记引用了该条目（R3 反向关联） */
    @GetMapping("source/notes")
    public Result sourceNotes(@RequestParam("sourceKey") String sourceKey) {
        Result result = Result.getInstance();
        List<Note> list = noteDataService.listPublicNotesBySource(sourceKey);
        result.setBody(list);
        return result;
    }

    /** 全量重建索引（仅超级管理员，幂等） */
    @PostMapping("rebuildIndex")
    public Result rebuildIndex(HttpSession session) {
        Result result = Result.getInstance();
        Integer role = (Integer) session.getAttribute("role");
        if (role == null || role != 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        try {
            result.setBody(noteDataService.rebuildIndex());
        } catch (IllegalStateException e) {
            return result.setErrorEnum(ErrorEnum.ERROR000021);
        }
        return result;
    }

    /**
     * 检索越权自检（仅超管）：以当前用户身份，用他人公开笔记的正文取样做一次正文检索。
     * pass=true 表示零命中 —— 即 Q1 决策（正文只对属主可检索）真实生效。
     * 注意：public 笔记正文本就必须入库（属主要检索自己的笔记），
     * 防线在查询条件，故本自检走真实检索路径而非查库计数。
     */
    @GetMapping("audit/foreignContent")
    public Result auditForeignContent(HttpSession session) {
        Result result = Result.getInstance();
        Integer role = (Integer) session.getAttribute("role");
        if (role == null || role != 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        result.setBody(noteSearchService.auditForeignContent(userId(session)));
        return result;
    }

    /** 旧笔记迁移：仅超级管理员，classpath data-note/*.md 拷入自己的笔记目录（同名跳过，幂等） */
    @PostMapping("migrateOld")
    public Result migrateOld(HttpSession session) {
        Result result = Result.getInstance();
        Integer role = (Integer) session.getAttribute("role");
        if (role == null || role != 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        int copied = noteDataService.migrateOld(userId(session));
        result.setBody(copied);
        return result;
    }

    /* ---------------- v2 P1：wiki 链接 / 导出 / 标签管理 ---------------- */

    /**
     * 按标题解析可见笔记（R7 wiki 链接跳转）：返回 ownerId 供前端 openDetail。
     * 自己的优先；不可见/不存在统一返回 null body，不泄露存在性。
     */
    @GetMapping("resolve")
    public Result resolve(@RequestParam("name") String name, HttpSession session) {
        Result result = Result.getInstance();
        result.setBody(noteDataService.resolveNote(userId(session), name));
        return result;
    }

    /** 反向链接（R7）：哪些可见笔记引用了 [[name]] */
    @GetMapping("backlinks")
    public Result backlinks(@RequestParam("name") String name, HttpSession session) {
        Result result = Result.getInstance();
        result.setBody(noteDataService.backlinks(userId(session), name));
        return result;
    }

    /**
     * 导出单篇笔记（R9）：md 文件下载。
     * 自己的任意笔记或他人的 public 笔记；无权/不存在报 000018。
     */
    @GetMapping("export")
    public ResponseEntity<byte[]> export(@RequestParam("ownerId") Long ownerId,
                                         @RequestParam("name") String name,
                                         HttpSession session) {
        if (ownerId == null) {
            return ResponseEntity.badRequest().build();
        }
        String md = noteDataService.exportNote(userId(session), ownerId, name);
        if (md == null) {
            return ResponseEntity.status(404).build();
        }
        String fname = name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".md";
        String encoded;
        try {
            // Java 8 无 encode(String, Charset) 重载，只能用 String 形式
            encoded = java.net.URLEncoder.encode(fname, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encoded = fname; // UTF-8 恒可用，理论不可达
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(md.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 导出自己的全部笔记（R9）：zip 下载，包内为原始 md 文件。
     */
    @GetMapping("exportAll")
    public ResponseEntity<byte[]> exportAll(HttpSession session) {
        List<String[]> files = noteDataService.exportAllNotes(userId(session));
        if (files.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bos,
                java.nio.charset.StandardCharsets.UTF_8)) {
            for (String[] f : files) {
                zip.putNextEntry(new java.util.zip.ZipEntry(f[0]));
                zip.write(f[1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (java.io.IOException e) {
            log.error("打包笔记导出失败", e);
            return ResponseEntity.status(500).build();
        }
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=wxyd-notes-" + stamp + ".zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(bos.toByteArray());
    }

    /**
     * 重命名/合并标签（R10）：只影响自己的笔记。to 为空 = 删除该标签。
     * 原生 fetch 下载场景少，这里仍走 jQuery 无妨；但下载链接必须原生 <a> 触发。
     */
    @PostMapping("tags/rename")
    public Result renameTag(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            int changed = noteDataService.renameTag(userId(session),
                    str(body.get("from")), str(body.get("to")));
            result.setBody(changed);
        } catch (IllegalArgumentException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    private static Long userId(HttpSession session) {
        Long id = (Long) session.getAttribute("userId");
        return id == null ? 0L : id;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** tags 允许数组或逗号分隔字符串两种形态 */
    @SuppressWarnings("unchecked")
    private static List<String> strList(Object o) {
        if (o == null) return null;
        List<String> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object x : (List<Object>) o) {
                if (x != null) out.add(String.valueOf(x));
            }
            return out.isEmpty() ? null : out;
        }
        for (String s : String.valueOf(o).split(",")) {
            if (!s.trim().isEmpty()) out.add(s.trim());
        }
        return out.isEmpty() ? null : out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(Result req) {
        if (req == null || !(req.getBody() instanceof Map)) {
            return new HashMap<>();
        }
        return (Map<String, Object>) req.getBody();
    }
}
