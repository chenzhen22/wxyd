package com.cyz.shellscript.controller;

import com.cyz.shellscript.model.ShellTopic;
import com.cyz.shellscript.service.ShellScriptDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Shell 脚本学习平台控制器。
 * <p>
 * 路径前缀 shellscript/topics（wxyd context-path 为 /api，完整路径 /api/shellscript/topics）。
 * 不实现 CommController，避免被 MockAspect 兜底拦截；鉴权由 AuthInterceptor 的 shellscript/** 作用域保障。
 * 与 {@code JavaApiController} 不同，本控制器不提供代码执行端点——纯只读 md 展示。
 */
@RestController
@RequestMapping("shellscript/topics")
public class ShellScriptController {

    @Autowired
    private ShellScriptDataService shellScriptDataService;

    @GetMapping
    public List<ShellTopic> getTopics() {
        return shellScriptDataService.getTopics();
    }

    @GetMapping("/{name}")
    public ResponseEntity<ShellTopic> getTopic(@PathVariable String name) {
        ShellTopic topic = shellScriptDataService.getTopic(name);
        if (topic == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(topic);
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload() {
        int count = shellScriptDataService.reload();
        return ResponseEntity.ok(Collections.singletonMap("loaded", count));
    }
}
