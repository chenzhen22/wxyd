package com.cyz.note.controller;

import com.cyz.constant.ErrorEnum;
import com.cyz.note.model.Note;
import com.cyz.note.service.NoteDataService;
import com.cyz.note.service.NoteSearchService;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
