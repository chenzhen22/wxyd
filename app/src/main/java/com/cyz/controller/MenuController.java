package com.cyz.controller;

import com.cyz.constant.ErrorEnum;
import com.cyz.pojo.Result;
import com.cyz.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 菜单管理控制器（仅管理员可读写配置，任何登录用户可查询自己可见的菜单）。
 * <p>
 * 路径前缀 menu（context-path /api），鉴权由 AuthInterceptor 的 /menu/** 保障。
 * 前端菜单的显隐为展示层过滤；管理员(role=0)始终可见全部菜单。
 */
@RestController
@Slf4j
public class MenuController implements CommController {

    @Autowired
    MenuService menuService;

    /** 全部菜单及配置（仅管理员） */
    @GetMapping("menu/all")
    public Result all(HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        return menuService.allConfigs();
    }

    /** 当前用户可见的菜单 key 列表 */
    @GetMapping("menu/visible")
    public Result visible(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        Integer role = (Integer) session.getAttribute("role");
        return menuService.visibleKeys(userId, role != null && role == 0);
    }

    /** 保存菜单可见性配置（仅管理员） */
    @PostMapping("menu/save")
    public Result save(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        Map<String, Object> body = req != null && req.getBody() instanceof Map
                ? (Map<String, Object>) req.getBody() : new java.util.HashMap<>();
        Object menuKey = body.get("menuKey");
        Object visible = body.get("visible");
        Object userIds = body.get("userIds");
        return menuService.save((Long) session.getAttribute("userId"),
                menuKey == null ? null : String.valueOf(menuKey),
                visible instanceof Number ? ((Number) visible).intValue() : null,
                userIds == null ? null : String.valueOf(userIds));
    }

    private boolean isAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role != null && role == 0;
    }
}
