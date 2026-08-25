package com.chenzhen.dispatcher;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.pojo.Result;
import com.chenzhen.service.*;
import com.chenzhen.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * In-process replacement for the former TbpApplyFeign + ./msg/*.send file queue.
 * Reads the {@code action} field from {@code result.getBody()} and calls the
 * corresponding service directly.
 * <p>
 * Only reached when {@code wxyd.mock.enabled=false}; in mock mode {@code MockAspect}
 * short-circuits every {@code tbphx.do} action before it gets here. The MySQL-backed
 * message actions and {@code getParamValue} are wired below; all other actions are
 * mock-only under this design and fall through to {@code default}, returning an empty
 * {@link Result}.
 */
@Component
@Slf4j
public class ActionDispatcher {

    @Autowired private MessageService messageService;
    @Autowired private LoginService loginService;
    @Autowired private UserService userService;

    public Result dispatch(Result result) {
        Object body = result.getBody();
        JSONObject json = body instanceof JSONObject ? (JSONObject) body
                : (JSONObject) JSONObject.toJSON(body);
        String action = json.getString("action");
        if (action != null) {
            json.remove("action");
        }
        result.setBody(json);
        try {
            if (action == null) {
                log.warn("[Dispatcher] no action in body");
                return result;
            }
            switch (action) {
                case "addMessage":
                    return messageService.addMessage(json.getString("message"));
                case "delMessage":
                    return messageService.delMessage(json.getString("msgId"));
                case "queryMessage":
                    return messageService.queryMessage(json.getString("flag"));
                case "getParamValue": {
                    Result r = Result.getInstance();
                    r.setTraceId(result.getTraceId());
                    r.setClientIp(result.getClientIp());
                    r.setBody(CommUtils.getParamValue(json.getString("key")));
                    return r;
                }
                default:
                    log.warn("[Dispatcher] action={} not wired for non-mock path; mock-only", action);
                    Result r = Result.getInstance();
                    r.setTraceId(result.getTraceId());
                    r.setClientIp(result.getClientIp());
                    return r;
            }
        } catch (Exception e) {
            log.error("[Dispatcher] action={} failed", action, e);
            return Result.getInstance();
        }
    }
}
