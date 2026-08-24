package com.chenzhen.mock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.pojo.Result;
import com.chenzhen.util.BtoAAtoB;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Order(1)
@Slf4j
public class MockAspect {

    @Value("${wxyd.mock.enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private MockDataService mockDataService;

    @javax.annotation.PostConstruct
    public void init() {
        log.info("[MOCK] MockAspect initialized, mockEnabled={}", mockEnabled);
    }

    @Around("execution(* com.chenzhen.controller.CommController+.*(..))")
    public Object mockAround(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!mockEnabled) {
            return joinPoint.proceed();
        }

        // 获取请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attrs.getRequest();

        // 归一化路径: /spbt/tbphx.do → tbphx.do
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String normalizedPath = requestUri.substring(contextPath.length());
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        // 获取请求参数
        Map<String, String> params = getParams(request, joinPoint);

        // ========== tbphx.do 特殊处理：解码 transData，按 action 路由 ==========
        if ("tbphx.do".equals(normalizedPath)) {
            String transData = params.get("transData");
            if (transData == null || transData.isEmpty()) {
                // 裸调用（无 transData）：返回 200 + 引导 JSON，避免 Spring 直接抛 400
                JSONObject guide = new JSONObject();
                guide.put("errorCode", "000001");
                guide.put("errorMsg", "transData required: GET /api/tbphx.do?transData=base64(URLEncode({\"action\":\"queryMsgCode\"}||<ts>))");
                log.info("[MOCK] tbphx.do 裸调用（无 transData），返回引导 JSON");
                return guide;
            }
            return handleTbphx(joinPoint, transData);
        }

        // 匹配 mock 数据（非 tbphx.do 路径）
        String mockJson = mockDataService.match(normalizedPath, params);

        if (mockJson == null) {
            log.warn("No mock data found for path: {}", normalizedPath);
            return joinPoint.proceed();
        }

        log.info("[MOCK] {} → {}", normalizedPath, mockJson);
        return buildResponse(joinPoint, mockJson);
    }

    /**
     * 处理 tbphx.do 请求：解码 transData，区分编码请求与业务请求
     */
    private Object handleTbphx(ProceedingJoinPoint joinPoint, String transData) throws Throwable {
        try {
            // 1. Base64 解码
            String decoded = BtoAAtoB.atob(transData);
            if (decoded == null) {
                log.warn("[MOCK] tbphx.do transData base64 decode failed");
                return joinPoint.proceed();
            }
            // 2. URL 解码
            decoded = URLDecoder.decode(decoded, "UTF-8");

            // 3. 判断是否编码请求（不含 || 时间戳）
            if (!decoded.contains("||")) {
                // === 编码请求：模拟服务端追加时间戳并重新编码 ===
                log.info("[MOCK] tbphx.do 编码请求: {}", decoded);
                String encoded = decoded + "||" + System.currentTimeMillis();
                encoded = URLEncoder.encode(encoded, "UTF-8");
                encoded = BtoAAtoB.btoa(encoded);

                Map<String, String> body = new HashMap<>();
                body.put("transData", encoded);
                Result result = Result.getInstance();
                result.setBody(body);
                log.info("[MOCK] tbphx.do 编码请求返回 → body.transData 已追加时间戳");
                return result;
            }

            // === 业务请求：解析 action，按 action 匹配 mock 数据 ===
            String[] parts = decoded.split("\\|\\|", -1);
            String jsonStr = parts[0];
            JSONObject json = JSONObject.parseObject(jsonStr);
            String action = json.getString("action");
            log.info("[MOCK] tbphx.do 业务请求 action={}, params={}", action, json);

            // 将解析出的所有字段作为匹配参数（优先用 action 匹配）
            Map<String, String> actionParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                if (entry.getValue() instanceof String && org.springframework.util.StringUtils.hasText((String) entry.getValue())) {
                    actionParams.put(entry.getKey(), (String) entry.getValue());
                }
            }
            // 确保 action 一定在 params 中
            if (!actionParams.containsKey("action")) {
                actionParams.put("action", action);
            }

            // 尝试匹配 mock/tbphx.do/action=xxx.json
            String mockJson = mockDataService.match("tbphx.do", actionParams);
            if (mockJson != null) {
                log.info("[MOCK] tbphx.do action={} → {}", action, mockJson);
                return buildResponseRaw(mockJson);
            }

            log.warn("[MOCK] tbphx.do 未找到 action={} 的 mock 数据，走默认", action);
            mockJson = mockDataService.match("tbphx.do", new HashMap<>());
            if (mockJson != null) {
                return buildResponseRaw(mockJson);
            }

            return joinPoint.proceed();
        } catch (Exception e) {
            log.warn("[MOCK] tbphx.do 处理异常: {}", e.getMessage());
            return joinPoint.proceed();
        }
    }

    /**
     * 构建 tbphx.do 业务请求的返回：直接返回 JSONObject 保留所有自定义字段（如 body、result 等）
     */
    private Object buildResponseRaw(String mockJson) {
        return JSONObject.parseObject(mockJson);
    }

    /**
     * 构建方法返回：void 方法写入 HttpServletResponse，非 void 返回 Result 对象
     */
    private Object buildResponse(ProceedingJoinPoint joinPoint, String mockJson) throws IOException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        if (returnType.equals(Void.TYPE)) {
            writeToResponse(joinPoint, mockJson);
            return null;
        } else {
            return JSONObject.parseObject(mockJson, Result.class);
        }
    }

    private Map<String, String> getParams(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        Map<String, String> params = new HashMap<>();
        // 从 query string 和 form body 获取
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap != null) {
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    params.put(entry.getKey(), values[0]);
                }
            }
        }

        // 检查是否有 @RequestBody Result 参数（取 body 中的第一个非空字段做匹配依据）
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Result) {
                    Result result = (Result) arg;
                    if (result.getBody() instanceof Map) {
                        Map<?, ?> bodyMap = (Map<?, ?>) result.getBody();
                        for (Map.Entry<?, ?> entry : bodyMap.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() instanceof String
                                    && !params.containsKey(entry.getKey().toString())) {
                                params.put(entry.getKey().toString(), (String) entry.getValue());
                            }
                        }
                    }
                }
            }
        }

        return params;
    }

    private void writeToResponse(ProceedingJoinPoint joinPoint, String mockJson) throws IOException {
        HttpServletResponse response = getResponseFromArgs(joinPoint);
        if (response == null) {
            log.warn("[MOCK] Cannot find HttpServletResponse in method args, mock skipped");
            return;
        }
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mockJson);
        response.getWriter().flush();
    }

    private HttpServletResponse getResponseFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof HttpServletResponse) {
                    return (HttpServletResponse) arg;
                }
            }
        }
        return null;
    }
}
