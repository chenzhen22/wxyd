package com.cyz.wbgateway.qoder;

import com.cyz.constant.ErrorEnum;
import com.cyz.mapper.gateway.WbGatewayMapper;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Qoder 专属管理端点（签到/额度）。
 * <p>
 * 放在 provider 包内，避免通用网关控制器出现厂商分支——新增 provider 时各自带自己的管理端点即可。
 * 路径仍在 /gateway/** 下，受登录拦截 + 管理员校验保护。
 */
@RestController
@RequestMapping("gateway/admin/qoder")
@Slf4j
public class QoderAdminController {

    @Autowired
    private QoderClient qoderClient;

    @Autowired
    private WbGatewayMapper wbGatewayMapper;

    private boolean notAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role == null || role != 0;
    }

    private static Long userId(HttpSession session) {
        Long id = (Long) session.getAttribute("userId");
        return id == null ? 0L : id;
    }

    /** 签到状态 + 自动领取开关 */
    @GetMapping("checkin")
    public Result checkin(HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        result.setBody(checkinBody());
        return result;
    }

    /** 立即领取（每天一轮，重复领取幂等） */
    @PostMapping("checkin/claim")
    public Result claim(HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        Map<String, Object> claim = qoderClient.claim();
        Map<String, Object> body = checkinBody();
        body.put("claim", claim);
        log.info("[QODER] 手动签到：{}（by user {}）", claim.get("message"), userId(session));
        result.setBody(body);
        return result;
    }

    /** 自动领取开关 */
    @PostMapping("checkin/enabled")
    public Result enabled(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (notAdmin(session)) return result.setErrorEnum(ErrorEnum.ERROR000014);
        Object raw = (req != null && req.getBody() instanceof Map)
                ? ((Map<?, ?>) req.getBody()).get("enabled") : null;
        boolean on = Boolean.TRUE.equals(raw) || "true".equalsIgnoreCase(String.valueOf(raw));
        wbGatewayMapper.updateQoderAutoCheckin(on ? 1 : 0);
        result.setBody(checkinBody());
        return result;
    }

    private Map<String, Object> checkinBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        Integer enabled = wbGatewayMapper.queryQoderAutoCheckin();
        body.put("enabled", enabled == null || enabled != 0);
        boolean signedIn = qoderClient.credential() != null;
        body.put("signedIn", signedIn);
        body.put("autoHours", "每天 10:10、20:10 各尝试一次（活动每天 10:00 刷新，重复领取不会重复发奖）");
        if (signedIn) {
            QoderClient.Checkin c = qoderClient.checkin(true);
            if (c != null) {
                body.put("active", c.active);
                body.put("todayCheckedIn", c.todayCheckedIn);
                body.put("amount", c.amount);
                body.put("unit", c.unit);
                body.put("validDays", c.validDays);
                body.put("endsAtMs", c.endsAtMs);
            }
            QoderClient.Usage u = qoderClient.usage();
            if (u != null) {
                body.put("credits", u.remaining);
                body.put("creditsTotal", u.total);
            }
        }
        return body;
    }
}
