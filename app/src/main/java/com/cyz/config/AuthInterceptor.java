package com.cyz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return true;
        }
        // ajax 请求返回 401；页面请求重定向 login.html
        String accept = req.getHeader("Accept");
        String xReq = req.getHeader("X-Requested-With");
        if ((accept != null && accept.contains("application/json")) || "XMLHttpRequest".equals(xReq)) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"errorCode\":\"000401\",\"errorMsg\":\"未登录或会话过期\"}");
        } else {
            String ctx = req.getContextPath();
            resp.sendRedirect(ctx + "/login.html");
        }
        return false;
    }
}
