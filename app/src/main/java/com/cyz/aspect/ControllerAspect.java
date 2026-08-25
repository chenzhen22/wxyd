package com.cyz.aspect;

import cn.hutool.core.util.StrUtil;
import com.cyz.pojo.Result;
import com.cyz.util.CommUtils;
import com.cyz.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class ControllerAspect {

    @Pointcut("execution(* com.cyz.controller.CommController+.*(..))")
    public void controllerLog() {}

    @Around("controllerLog()")
    public Object logAroud(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        for(Object obj : args) {
            if(obj instanceof Result) {
                Result result = (Result) obj;
                String traceId = result.getTraceId();
                String clientIp = result.getClientIp();
                if (StrUtil.isEmpty(traceId)) {
                    traceId = LogUtil.getTraceId();
                }
                if (StrUtil.isEmpty(clientIp)) {
                    // 直连接口（前端未带 clientIp）：从当前请求解析真实客户端 IP，避免留言板等依赖 clientIp 的逻辑拿到 null
                    clientIp = resolveClientIp();
                }
                MDC.put("traceId", traceId);
                MDC.put("clientIp", clientIp);
            }
        }
        log.info("==== 接口开始 ==== 方法:{} 入参:{}", methodName, args);
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("==== 接口成功 ==== 方法:{} 耗时:{}ms 返回:{}", methodName, cost, result);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            log.info("==== 接口异常 ==== 方法:{} 耗时:{}ms 异常:{}", methodName, cost, e);
            throw e;
        }
    }

    /**
     * 从当前请求解析客户端 IP（直连接口前端未带 clientIp 时使用）。
     */
    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return CommUtils.getClientIp(attrs.getRequest());
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
