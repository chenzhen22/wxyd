package com.cyz.radar.controller;

import com.cyz.constant.ErrorEnum;
import com.cyz.pojo.Result;
import com.cyz.radar.model.RadarSubscribe;
import com.cyz.radar.service.RadarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 羊毛雷达控制器（/radar/**，AuthInterceptor 鉴权）。
 * 不实现 CommController（避免 MockAspect 兜底拦截，与 NoteController 同模式）。
 */
@RestController
@RequestMapping("radar")
@Slf4j
public class RadarController {

    @Autowired
    private RadarService radarService;

    @GetMapping("list")
    public Result list(HttpSession session) {
        Result result = Result.getInstance();
        result.setBody(radarService.list(userId(session)));
        return result;
    }

    @PostMapping("add")
    public Result add(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        try {
            RadarSubscribe s = radarService.add(userId(session),
                    str(body.get("keyword")), str(body.get("channel")),
                    longVal(body.get("robotId")), str(body.get("email")));
            result.setBody(s);
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
        if (!radarService.delete(userId(session), longVal(body.get("id")))) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        return result;
    }

    @PostMapping("toggle")
    public Result toggle(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        Map<String, Object> body = body(req);
        Boolean enabled = boolVal(body.get("enabled"));
        if (!radarService.toggle(userId(session), longVal(body.get("id")), Boolean.TRUE.equals(enabled))) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        return result;
    }

    @PostMapping("test")
    public Result test(@RequestBody(required = false) Result req, HttpSession session) {
        Map<String, Object> body = body(req);
        return radarService.test(userId(session), longVal(body.get("id")));
    }

    private static Long userId(HttpSession session) {
        Long id = (Long) session.getAttribute("userId");
        return id == null ? 0L : id;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
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

    private static Boolean boolVal(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.valueOf(String.valueOf(o));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(Result req) {
        if (req == null || !(req.getBody() instanceof Map)) {
            return new java.util.HashMap<>();
        }
        return (Map<String, Object>) req.getBody();
    }
}
