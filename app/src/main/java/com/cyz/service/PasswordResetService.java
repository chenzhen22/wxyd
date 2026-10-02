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
 * 忘记密码流程：发码（凭滑块 ticket，邮箱须已注册）→ 验码（签发一次性 resetToken）→ 重置密码。
 * 验证码/token 均存内存（单实例部署）；验证码 5 分钟有效、60 秒重发间隔；
 * token 10 分钟有效、一次性且绑定邮箱。
 */
@Service
@Slf4j
public class PasswordResetService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final long CODE_TTL = 5 * 60 * 1000L;
    private static final long SEND_INTERVAL = 60 * 1000L;
    private static final long TOKEN_TTL = 10 * 60 * 1000L;

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
    /** resetToken -> 绑定邮箱 */
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

    /** 发送重置验证码：先消费滑块 ticket，再校验邮箱已绑定账号，最后发 6 位数字码。 */
    public void sendCode(String email, String ticket) {
        if (!sliderCaptchaService.consumeTicket(ticket)) {
            throw new IllegalArgumentException("滑块验证无效，请重新验证");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        email = email.trim().toLowerCase();
        if (mysqlMapper.queryUserByEmail(email) == null) {
            throw new IllegalArgumentException("该邮箱未绑定任何账号");
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

        String content = "您好！\n\n您正在重置小泽助手的登录密码。\n\n本次验证码为：" + code
                + "，5 分钟内有效。请勿泄露给他人。\n\n如非本人操作，请忽略本邮件。\n\n—— 小泽助手";
        if (mockEnabled) {
            log.info("[MOCK] 密码重置验证码邮件（mock 模式不真实发送）：email={} code={}", email, code);
        } else {
            mailService.send(email, "小泽助手密码重置验证码", content);
        }
    }

    /** 校验邮箱验证码；通过则销毁验证码并签发绑定邮箱的一次性 resetToken。 */
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

    /** 重置密码：resetToken 有效则更新对应用户密码（SM4 加密存储）。 */
    public void reset(String token, String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码长度至少 6 位");
        }
        TokenEntry t = token == null ? null : tokenStore.remove(token);
        if (t == null || t.getExpireAt() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("重置会话已过期，请重新验证邮箱");
        }
        String email = t.getEmail();
        User u = mysqlMapper.queryUserByEmail(email);
        if (u == null) {
            throw new IllegalArgumentException("该邮箱未绑定任何账号");
        }
        mysqlMapper.updateUserPassword(u.getId(), sm4KeyHolder.encrypt(password));
        log.info("密码重置成功：email={} userId={}", email, u.getId());
    }
}
