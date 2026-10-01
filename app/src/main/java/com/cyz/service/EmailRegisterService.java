package com.cyz.service;

import com.cyz.config.Sm4KeyHolder;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.User;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 邮箱注册流程：发码（凭滑块 ticket）→ 验码（签发一次性 registerToken）→ 完成注册。
 * 验证码/token 均存内存（单实例部署）；验证码 5 分钟有效、60 秒重发间隔；
 * token 10 分钟有效、一次性且绑定邮箱。注册用户免审批（status=0）。
 */
@Service
@Slf4j
public class EmailRegisterService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final long CODE_TTL = 5 * 60 * 1000L;
    private static final long SEND_INTERVAL = 60 * 1000L;
    private static final long TOKEN_TTL = 10 * 60 * 1000L;
    private static final String CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    @Autowired
    private MysqlMapper mysqlMapper;

    @Autowired
    private SliderCaptchaService sliderCaptchaService;

    @Autowired
    private MailService mailService;

    @Autowired
    private Sm4KeyHolder sm4KeyHolder;

    @Value("${wxyd.mock.enabled:false}")
    private boolean mockEnabled;

    /** email -> 验证码条目 */
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();
    /** registerToken -> 绑定邮箱 */
    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Data
    private static class CodeEntry {
        private String code;
        private long expireAt;
        private long lastSentAt;
    }

    @Data
    private static class TokenEntry {
        private String email;
        private long expireAt;
    }

    /** 发送注册验证码：先消费滑块 ticket，再校验邮箱未注册，最后发 6 位数字码。 */
    public void sendCode(String email, String ticket) {
        if (!sliderCaptchaService.consumeTicket(ticket)) {
            throw new IllegalArgumentException("滑块验证无效，请重新验证");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        email = email.trim().toLowerCase();
        if (mysqlMapper.queryUserByEmail(email) != null) {
            throw new IllegalArgumentException("该邮箱已注册，请直接登录");
        }
        CodeEntry old = codeStore.get(email);
        if (old != null && System.currentTimeMillis() - old.getLastSentAt() < SEND_INTERVAL) {
            throw new IllegalArgumentException("发送太频繁，请 1 分钟后再试");
        }
        String code = String.format("%06d", random.nextInt(1000000));
        CodeEntry entry = new CodeEntry();
        entry.setCode(code);
        entry.setExpireAt(System.currentTimeMillis() + CODE_TTL);
        entry.setLastSentAt(System.currentTimeMillis());
        codeStore.put(email, entry);

        String content = "您好！\n\n欢迎注册小泽助手。\n\n本次注册验证码为：" + code
                + "，5 分钟内有效。请勿泄露给他人。\n\n如非本人操作，请忽略本邮件。\n\n—— 小泽助手";
        if (mockEnabled) {
            log.info("[MOCK] 注册验证码邮件（mock 模式不真实发送）：email={} code={}", email, code);
        } else {
            mailService.send(email, "小泽助手注册验证码", content);
        }
    }

    /** 校验邮箱验证码；通过则销毁验证码并签发绑定邮箱的一次性 registerToken。 */
    public String verifyCode(String email, String code) {
        if (email == null || code == null) {
            throw new IllegalArgumentException("参数缺失");
        }
        email = email.trim().toLowerCase();
        CodeEntry entry = codeStore.get(email);
        if (entry == null || entry.getExpireAt() < System.currentTimeMillis()
                || !entry.getCode().equals(code.trim())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        codeStore.remove(email);
        String token = UUID.randomUUID().toString().replace("-", "");
        TokenEntry t = new TokenEntry();
        t.setEmail(email);
        t.setExpireAt(System.currentTimeMillis() + TOKEN_TTL);
        tokenStore.put(token, t);
        return token;
    }

    /** 完成注册：registerToken + 密码 + 用户名（选填，缺省自动生成 10 位英文+数字）。 */
    public User complete(String token, String password, String username) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码长度至少 6 位");
        }
        TokenEntry t = token == null ? null : tokenStore.remove(token);
        if (t == null || t.getExpireAt() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("注册会话已过期，请重新验证邮箱");
        }
        String email = t.getEmail();
        if (mysqlMapper.queryUserByEmail(email) != null) {
            throw new IllegalArgumentException("该邮箱已注册，请直接登录");
        }
        String finalUsername;
        if (username != null && !username.trim().isEmpty()) {
            finalUsername = username.trim();
            if (!finalUsername.matches("^[A-Za-z0-9_\\-\\u4e00-\\u9fa5]{2,32}$")) {
                throw new IllegalArgumentException("用户名须为 2-32 位中英文、数字、下划线或中划线");
            }
            if (mysqlMapper.queryUserByUsername(finalUsername) != null) {
                throw new IllegalArgumentException("用户名已存在");
            }
        } else {
            finalUsername = generateUsername();
        }
        User u = new User();
        u.setUsername(finalUsername);
        u.setEmail(email);
        u.setPassword(sm4KeyHolder.encrypt(password));
        u.setDisplayName(finalUsername);
        u.setRole(1);
        u.setStatus(0); // 邮箱已验证，免审批
        mysqlMapper.insertUser(u);
        u.setPassword(null);
        log.info("邮箱注册成功：email={} username={}", email, finalUsername);
        return u;
    }

    /** 生成 10 位英文+数字用户名，撞唯一约束则重试。 */
    private String generateUsername() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder("u");
            for (int j = 0; j < 9; j++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            String candidate = sb.toString();
            if (mysqlMapper.queryUserByUsername(candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("用户名生成失败，请重试");
    }
}
