package com.cyz.patrol.controller;

import com.cyz.constant.ErrorEnum;
import com.cyz.patrol.service.PatrolService;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 巡检日报控制器（/patrol/**，AuthInterceptor 鉴权）。
 * 不实现 CommController（与 Note/Radar 同模式）。
 */
@RestController
@RequestMapping("patrol")
@Slf4j
public class PatrolController {

    @Autowired
    private PatrolService patrolService;

    /**
     * 🔒 仅管理员可用（产品决策 2026-10-10）：巡检资产含 SSH 凭据等敏感信息。
     * 菜单隐藏只是展示层，接口必须真拦截 —— 所有端点统一走此守卫。
     */
    private boolean notAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role == null || role != 0;
    }

    /** 最近一次系统自检结果（从未巡检过返回 null，前端显示"尚未巡检"） */
    @GetMapping("status")
    public Result status(HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        result.setBody(patrolService.getStatus());
        return result;
    }

    /** 立即巡检固定四项，返回报告（同时刷新内存最近结果） */
    @PostMapping("runNow")
    public Result runNow(HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        result.setBody(patrolService.runAll(userId(session)));
        return result;
    }

    /** 测试推送：立即巡检 + 按当前设置推一条日报（不看推送时间），验证全链路 */
    @PostMapping("testPush")
    public Result testPush(HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        String err = patrolService.testPush(userId(session));
        if (err != null) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(err);
        } else {
            result.setBody("推送成功，请查收");
        }
        return result;
    }

    @GetMapping("setting")
    public Result setting(HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        Map<String, Object> s = patrolService.getSetting(userId(session));
        result.setBody(s == null ? new HashMap<>() : s);
        return result;
    }

    @PostMapping("setting")
    public Result saveSetting(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        Map<String, Object> body = body(req);
        try {
            patrolService.saveSetting(userId(session), intVal(body.get("pushHour")),
                    str(body.get("channel")), longVal(body.get("robotId")), str(body.get("email")));
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

    private static Integer intVal(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long longVal(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(Result req) {
        if (req == null || !(req.getBody() instanceof Map)) {
            return new HashMap<>();
        }
        return (Map<String, Object>) req.getBody();
    }
}
