package com.cyz.controller;

import com.cyz.constant.ErrorEnum;
import com.cyz.pojo.Result;
import com.cyz.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 菜单管理控制器（仅管理员可读写配置与菜单项，任何登录用户可查询自己可见的菜单）。
 * <p>
 * 路径前缀 menu（context-path /api），鉴权由 AuthInterceptor 的 /menu/** 保障。
 * 前端菜单的显隐为展示层过滤；管理员(role=0)始终可见全部菜单。
 */
@RestController
@Slf4j
public class MenuController implements CommController {

    @Autowired
    MenuService menuService;

    /** 全部菜单及权限配置（仅管理员，菜单管理页用） */
    @GetMapping("menu/all")
    public Result all(@RequestParam(defaultValue = "desktop") String platform, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        return menuService.allConfigs(platform);
    }

    /** 当前用户可见的菜单 key 列表（desktop 平台，兼容老前端） */
    @Deprecated // 已由 menu/list 取代
    @GetMapping("menu/visible")
    public Result visible(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        Integer role = (Integer) session.getAttribute("role");
        return menuService.visibleKeys(userId, role != null && role == 0);
    }

    /** 当前用户在指定平台（h5|desktop）可见的菜单列表 */
    @GetMapping("menu/list")
    public Result list(@RequestParam String platform, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        Integer role = (Integer) session.getAttribute("role");
        return menuService.list(platform, userId, role != null && role == 0);
    }

    /** 保存菜单可见性配置（仅管理员） */
    @PostMapping("menu/save")
    public Result save(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        Map<String, Object> body = bodyMap(req);
        Object menuKey = body.get("menuKey");
        Object visible = body.get("visible");
        Object userIds = body.get("userIds");
        Object platform = body.get("platform");
        return menuService.save((Long) session.getAttribute("userId"),
                platform == null ? "desktop" : String.valueOf(platform),
                menuKey == null ? null : String.valueOf(menuKey),
                visible instanceof Number ? ((Number) visible).intValue() : null,
                userIds == null ? null : String.valueOf(userIds));
    }

    /** 新增/编辑菜单项（仅管理员；存在则更新，否则插入） */
    @PostMapping("menu/item/save")
    public Result saveItem(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        Map<String, Object> body = bodyMap(req);
        return menuService.saveItem((Long) session.getAttribute("userId"),
                str(body.get("platform")),
                str(body.get("menuKey")),
                str(body.get("name")),
                str(body.get("icon")),
                str(body.get("hashUrl")),
                toInt(body.get("sortNo")),
                toInt(body.get("adminOnly")));
    }

    /** 删除菜单项（仅管理员；fixed=1 禁止；连带删权限配置） */
    @PostMapping("menu/item/delete")
    public Result deleteItem(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        Map<String, Object> body = bodyMap(req);
        return menuService.deleteItem((Long) session.getAttribute("userId"),
                str(body.get("platform")), str(body.get("menuKey")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> bodyMap(Result req) {
        return req != null && req.getBody() instanceof Map
                ? (Map<String, Object>) req.getBody() : new HashMap<>();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** Number 或数字字符串 → Integer；解析失败返回 null */
    private static Integer toInt(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role != null && role == 0;
    }
}
