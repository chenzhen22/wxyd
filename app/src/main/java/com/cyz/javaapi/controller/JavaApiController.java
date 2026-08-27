package com.cyz.javaapi.controller;

import com.cyz.javaapi.model.ApiClass;
import com.cyz.javaapi.model.CodeResult;
import com.cyz.javaapi.service.ApiDataService;
import com.cyz.javaapi.service.CodeExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Java API 学习平台控制器。
 * <p>
 * 路径前缀 javaapi/classes（wxyd context-path 为 /api，完整路径 /api/javaapi/classes）。
 * 不实现 CommController，避免被 MockAspect 兜底拦截；鉴权由 AuthInterceptor 的 javaapi/** 作用域保障。
 */
@RestController
@RequestMapping("javaapi/classes")
public class JavaApiController {

    @Autowired
    private ApiDataService apiDataService;

    @Autowired
    private CodeExecutionService codeExecutionService;

    @GetMapping
    public List<ApiClass> getApiClasses() {
        return apiDataService.getApiClasses();
    }

    @GetMapping("/{name}")
    public ResponseEntity<ApiClass> getApiClass(@PathVariable String name) {
        ApiClass apiClass = apiDataService.getApiClass(name);
        if (apiClass == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(apiClass);
    }

    @PostMapping("/{name}/test")
    public ResponseEntity<CodeResult> runTest(
            @PathVariable String name,
            @RequestBody Map<String, String> request) {
        ApiClass apiClass = apiDataService.getApiClass(name);
        if (apiClass == null) {
            return ResponseEntity.notFound().build();
        }

        String testName = request.get("testName");
        String code = request.get("code");

        if (code == null || code.trim().isEmpty()) {
            CodeResult errorResult = new CodeResult(false, false, "", "代码不能为空", 0);
            return ResponseEntity.badRequest().body(errorResult);
        }

        CodeResult result = codeExecutionService.execute(testName, code);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload() {
        int count = apiDataService.reload();
        return ResponseEntity.ok(Collections.singletonMap("loaded", count));
    }
}
