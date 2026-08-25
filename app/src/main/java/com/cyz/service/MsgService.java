package com.chenzhen.service;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.dispatcher.ActionDispatcher;
import com.chenzhen.pojo.Result;
import com.chenzhen.util.CommUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Was a static utility that wrote {@code ./msg/*.send} files for the batch module to
 * pick up and Feign-call tbpApply. Now a Spring bean that dispatches in-process via
 * {@link ActionDispatcher} (no files, no Feign).
 *
 * <p>Mock mode ({@code wxyd.mock.enabled=true}) short-circuits {@code tbphx.do} at
 * {@code MockAspect} before {@link #getResult(JSONObject)} is reached.
 */
@Component
public class MsgService {

    @Autowired
    private ActionDispatcher actionDispatcher;

    public Result getResult(JSONObject json) {
        json.put("traceId", MDC.get("traceId"));
        json.put("clientIp", MDC.get("clientIp"));
        Result result = Result.getInstance();
        result.setBody(json);
        return actionDispatcher.dispatch(result);
    }

    /**
     * Reads a config value from {@code common.properties} via {@link CommUtils}.
     * Used by the {@code /uploadDoc} endpoint (not a {@code tbphx.do} action, so
     * MockAspect does not short-circuit it) to gate the upload IP whitelist.
     */
    public String getParamValue(String key) {
        return CommUtils.getParamValue(key);
    }
}
