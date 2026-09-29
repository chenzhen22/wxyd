package com.cyz.note.controller;

import com.cyz.constant.ErrorEnum;
import com.cyz.note.model.Note;
import com.cyz.note.service.NoteDataService;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 记忆笔记控制器（用户级，可编辑）。
 * <p>
 * 路径前缀 note（wxyd context-path 为 /api，完整路径 /api/note/**），
 * 鉴权由 AuthInterceptor 的 /note/** 作用域保障（必须登录）。
 * 不实现 CommController，避免被 MockAspect 兜底拦截。
 */
@RestController
@RequestMapping("note")
@Slf4j
public class NoteController {

    @Autowired
    private NoteDataService noteDataService;

    /** 我的笔记列表 */
    @GetMapping("list")
    public Result list(HttpSession session) {
        Result result = Result.getInstance();
        List<Note> list = noteDataService.listMyNotes(userId(session));
        result.setBody(list);
        return result;
    }

    /** 按标题模糊搜索公共笔记 */
    @GetMapping("search")
    public Result search(@RequestParam(value = "keyword", required = false) String keyword) {
        Result result = Result.getInstance();
        result.setBody(noteDataService.searchPublic(keyword));
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
                    str(body.get("oldName")));
            result.setBody(title);
        } catch (IllegalArgumentException e) {
            result.setErrorMsg(e.getMessage());
            result.setErrorCode("000015");
        } catch (IllegalStateException e) {
            result.setErrorMsg(e.getMessage());
            result.setErrorCode("000015");
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
        return (Long) session.getAttribute("userId");
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Map<String, Object> body(Result req) {
        if (req == null || !(req.getBody() instanceof Map)) {
            return new java.util.HashMap<>();
        }
        return (Map<String, Object>) req.getBody();
    }
}
