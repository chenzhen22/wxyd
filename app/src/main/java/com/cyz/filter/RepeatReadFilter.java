package com.cyz.filter;

import com.cyz.request.RepeatReadRequestWrapper;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

//@WebFilter(filterName = "repeatReadFilter", urlPatterns = "/*")
@Component
public class RepeatReadFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            String contentType = req.getContentType();
            // multipart 请求（文件上传）不包装 body，否则会提前消费输入流导致容器无法解析表单字段与文件
            if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
                chain.doFilter(request, response);
                return;
            }
            request = new RepeatReadRequestWrapper(req);
        }
        chain.doFilter(request, response);
    }
}
