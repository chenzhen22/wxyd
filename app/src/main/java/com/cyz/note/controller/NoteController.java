package com.cyz.note.controller;

import com.cyz.note.model.Note;
import com.cyz.note.service.NoteDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 记忆笔记控制器。
 * <p>
 * 路径前缀 note（wxyd context-path 为 /api，完整路径 /api/note）。
 * 不实现 CommController，避免被 MockAspect 兜底拦截；鉴权由 AuthInterceptor 的 note/** 作用域保障。
 * 与 {@code ShellScriptController} 类似，本控制器为纯只读 markdown 展示。
 */
@RestController
@RequestMapping("note")
public class NoteController {

    @Autowired
    private NoteDataService noteDataService;

    @GetMapping
    public List<Note> list() {
        return noteDataService.getNotes();
    }

    @GetMapping("/{name}")
    public ResponseEntity<Note> get(@PathVariable String name) {
        Note note = noteDataService.getNote(name);
        if (note == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(note);
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload() {
        int count = noteDataService.reload();
        return ResponseEntity.ok(Collections.singletonMap("loaded", count));
    }
}
