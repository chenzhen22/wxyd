package com.chenzhen.interceptor;

import com.chenzhen.util.CommUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

public class FeignInterceptor implements RequestInterceptor {

    @Autowired
    private HttpServletRequest request;

    @Override
    public void apply(RequestTemplate template) {
        if(request != null) {
            template.header("X-Forwarded-For", CommUtils.getClientIp(request));
        }
    }
}
