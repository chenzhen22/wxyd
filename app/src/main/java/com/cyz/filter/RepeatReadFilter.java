package com.chenzhen.filter;

import com.chenzhen.request.RepeatReadRequestWrapper;
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
            request = new RepeatReadRequestWrapper((HttpServletRequest) request);
        }
        chain.doFilter(request, response);
    }
}
