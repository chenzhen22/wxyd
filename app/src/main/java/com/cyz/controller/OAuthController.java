package com.cyz.controller;

import com.cyz.pojo.User;
import com.cyz.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * GitHub OAuth2 授权登录（仅管理员/团队自助登录，与密码登录并存）。
 * <p>
 * 流程：/oauth/github（发起）→ GitHub 授权页 → /oauth/github/callback（换 token、取用户信息、自动建号/登录、写 session）。
 * 两个端点均未被 AuthInterceptor 守护（不在拦截路径内），回调通过浏览器 302 跳转而非 XHR。
 */
@RestController
@RequestMapping("oauth")
@Slf4j
public class OAuthController implements CommController {

    private static final String GITHUB_AUTHORIZE = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_API_USER = "https://api.github.com/user";
    private static final String SESSION_STATE = "githubOauthState";
    private static final String SESSION_REDIRECT = "githubOauthRedirect";

    @Value("${wxyd.github.client-id:}")
    private String clientId;

    @Value("${wxyd.github.client-secret:}")
    private String clientSecret;

    @Value("${wxyd.github.redirect-uri:}")
    private String redirectUri;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private final AuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();

    public OAuthController(AuthService authService) {
        this.authService = authService;
    }

    private boolean enabled() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty()
                && redirectUri != null && !redirectUri.isEmpty();
    }

    /** 发起授权：生成 state 存入 session，重定向到 GitHub 授权页 */
    @GetMapping("github")
    public void github(@RequestParam(value = "redirect", required = false) String redirect,
                       HttpSession session, HttpServletResponse response) throws IOException {
        if (!enabled()) {
            response.sendRedirect(safeRedirectTarget("/api/login.html?oauth=disabled"));
            return;
        }
        String state = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        session.setAttribute(SESSION_STATE, state);
        // 仅允许站内路径（/api/ 开头），防开放跳转
        String safeRedirect = (redirect != null && redirect.startsWith("/api/")) ? redirect : "/api/";
        session.setAttribute(SESSION_REDIRECT, safeRedirect);

        String url = GITHUB_AUTHORIZE + "?"
                + "client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&scope=" + enc("read:user")
                + "&state=" + enc(state);
        response.sendRedirect(url);
    }

    /** 授权回调：校验 state、换 token、取用户信息、自动建号/登录、写 session、跳回原页面 */
    @GetMapping("github/callback")
    public void githubCallback(@RequestParam(value = "code", required = false) String code,
                               @RequestParam(value = "state", required = false) String state,
                               HttpSession session, HttpServletResponse response) throws IOException {
        String savedState = (String) session.getAttribute(SESSION_STATE);
        String redirect = (String) session.getAttribute(SESSION_REDIRECT);
        session.removeAttribute(SESSION_STATE);
        session.removeAttribute(SESSION_REDIRECT);

        if (!enabled()) {
            response.sendRedirect(safeRedirectTarget("/api/login.html?oauth=disabled"));
            return;
        }
        if (code == null || code.isEmpty() || savedState == null || !savedState.equals(state)) {
            response.sendRedirect(safeRedirectTarget("/api/login.html?oauth=error"));
            return;
        }

        try {
            String accessToken = exchangeToken(code);
            Map<String, Object> profile = fetchUserProfile(accessToken);
            String login = str(profile.get("login"));
            String name = str(profile.get("name"));
            if (login == null || login.isEmpty()) {
                response.sendRedirect(safeRedirectTarget("/api/login.html?oauth=error"));
                return;
            }
            User u = authService.findOrCreateByGithub(login, name);
            session.setAttribute("userId", u.getId());
            session.setAttribute("username", u.getUsername());
            session.setAttribute("role", u.getRole());
            response.sendRedirect(safeRedirectTarget(redirect != null ? redirect : "/api/"));
        } catch (Exception e) {
            log.error("GitHub 授权登录失败", e);
            response.sendRedirect(safeRedirectTarget("/api/login.html?oauth=error"));
        }
    }

    private String exchangeToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        org.springframework.util.LinkedMultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        HttpEntity<org.springframework.util.MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(GITHUB_TOKEN, entity, Map.class);
        Map<String, Object> map = resp.getBody();
        if (map == null || map.get("access_token") == null) {
            throw new IllegalStateException("GitHub 返回无 access_token：" + map);
        }
        return String.valueOf(map.get("access_token"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.Collections.singletonList(MediaType.parseMediaType("application/vnd.github+json")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> resp = restTemplate.exchange(GITHUB_API_USER, HttpMethod.GET, entity, Map.class);
        return resp.getBody();
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return s; // UTF-8 始终可用，不会走到这里
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** 统一把站内相对路径补齐 context-path 并返回，越界则回登录页 */
    private String safeRedirectTarget(String target) {
        if (target == null || !target.startsWith("/api/")) {
            target = "/api/login.html?oauth=error";
        }
        return target;
    }
}
