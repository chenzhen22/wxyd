package com.cyz.dubbo.controller;

import com.cyz.dubbo.model.CallResult;
import com.cyz.dubbo.model.DubboConfig;
import com.cyz.dubbo.service.DubboInvoker;
import com.cyz.util.CommUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Dubbo 调用测试工具控制器。
 * <p>
 * 路径前缀 dubbo（wxyd context-path 为 /api，完整路径 /api/dubbo/invoke）。
 * 不实现 CommController，避免被 MockAspect 兜底拦截；鉴权由 AuthInterceptor 的 /dubbo/** 作用域保障。
 */
@RestController
@RequestMapping("dubbo")
public class DubboController {

    private final DubboInvoker invoker;

    @Autowired
    public DubboController(DubboInvoker invoker) {
        this.invoker = invoker;
    }

    @PostMapping("invoke")
    public ResponseEntity<CallResult> invoke(@RequestBody DubboConfig config,
                                             HttpServletRequest request) {
        CallResult cr = new CallResult();
        cr.setConfigId(config.getId());
        cr.setConfigName(config.getName());
        long start = System.currentTimeMillis();
        try {
            Object result = invoker.invoke(config, CommUtils.getClientIp(request));
            cr.setSuccess(true);
            cr.setCostMs(System.currentTimeMillis() - start);
            cr.setResponse(result);
        } catch (Exception e) {
            cr.setSuccess(false);
            cr.setCostMs(System.currentTimeMillis() - start);
            cr.setError(e.toString() + (e.getMessage() != null ? " | " + e.getMessage() : ""));
        }
        return ResponseEntity.ok(cr);
    }
}
