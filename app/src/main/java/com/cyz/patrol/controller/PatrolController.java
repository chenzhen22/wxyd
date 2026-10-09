package com.cyz.patrol.controller;

import com.cyz.constant.ErrorEnum;
import com.cyz.patrol.model.PatrolAsset;
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

    @GetMapping("list")
    public Result list(HttpSession session) {
        Result result = Result.getInstance();
        result.setBody(patrolService.list(userId(session)));
        return result;
    }

    @PostMapping("add")
    public Result add(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            PatrolAsset a = patrolService.add(userId(session),
                    str(body.get("name")), str(body.get("assetType")), str(body.get("target")),
                    intVal(body.get("port")), str(body.get("sshUser")), str(body.get("sshPassword")));
            a.setSshPassword(null); // 密文永不下发
            result.setBody(a);
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.setErrorEnum(ErrorEnum.ERROR000015);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    @PostMapping("delete")
    public Result delete(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        if (!patrolService.delete(userId(session), longVal(body.get("id")))) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        return result;
    }

    /** 立即巡检全部启用资产，返回结果（同时更新 last_*，页面可见） */
    @PostMapping("runNow")
    public Result runNow(HttpSession session) {
        Result result = Result.getInstance();
        result.setBody(patrolService.runForUser(userId(session)));
        return result;
    }

    @GetMapping("setting")
    public Result setting(HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> s = patrolService.getSetting(userId(session));
        result.setBody(s == null ? new HashMap<>() : s);
        return result;
    }

    @PostMapping("setting")
    public Result saveSetting(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
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
