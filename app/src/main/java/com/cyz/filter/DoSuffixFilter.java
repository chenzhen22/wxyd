package com.cyz.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 统一 .do 后缀支持：把 /api/xxx.do 剥掉后缀转发到 /api/xxx。
 * <p>
 * 为什么不用 MVC 后缀匹配（use-suffix-pattern）：本项目存在 /login、/register 等
 * 控制器与 login.html/register.html 静态页同名，开启后 /login.html 会命中 login
 * 控制器导致循环视图 500，故改为过滤器显式剥后缀。
 * <p>
 * 豁免：/tbphx.do 为遗留外部网关的字面映射（见 ApiController），不能剥。
 * 转发为 FORWARD dispatch，不会再进入本过滤器（Filter 默认只拦 REQUEST）。
 */
@Component
@Slf4j
public class DoSuffixFilter implements Filter {

    /** 遗留外部网关，字面 .do 映射，必须豁免 */
    private static final String LEGACY_GATEWAY = "/tbphx.do";

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
        if (path.length() > 3 && path.endsWith(".do") && !LEGACY_GATEWAY.equals(path)) {
            String target = path.substring(0, path.length() - 3);
            request.getRequestDispatcher(target).forward(request, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
