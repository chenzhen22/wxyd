package com.chenzhen.aspect;

import cn.hutool.core.util.StrUtil;
import com.chenzhen.pojo.Result;
import com.chenzhen.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ControllerAspect {

    @Pointcut("execution(* com.chenzhen.controller.CommController+.*(..))")
    public void controllerLog() {}

    @Around("controllerLog()")
    public Object logAroud(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        for(Object obj : args) {
            if(obj instanceof Result) {
                Result result = (Result) obj;
                String traceId = result.getTraceId();
                if (StrUtil.isEmpty(traceId)) {
                    traceId = LogUtil.getTraceId();
                }
                MDC.put("traceId", traceId);
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
}
