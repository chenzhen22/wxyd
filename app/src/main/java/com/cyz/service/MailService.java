package com.cyz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

/**
 * SMTP 邮件发送（验证码等场景），配置来自 env.properties 的 wxyd.smtp.*。
 * 端口 465 走 SSL，其他端口走 STARTTLS；均设置连接/读超时。
 */
@Service
@Slf4j
public class MailService {

    @Value("${wxyd.smtp.host:}")
    private String host;

    @Value("${wxyd.smtp.port:465}")
    private int port;

    @Value("${wxyd.smtp.username:}")
    private String username;

    @Value("${wxyd.smtp.password:}")
    private String password;

    @Value("${wxyd.smtp.from:}")
    private String from;

    public void send(String to, String subject, String content) {
        sendInternal(to, subject, content, false);
    }

    /** HTML 格式邮件（正文为 HTML 片段，可含 <a> 链接等） */
    public void sendHtml(String to, String subject, String html) {
        sendInternal(to, subject, html, true);
    }

    private void sendInternal(String to, String subject, String content, boolean html) {
        if (host == null || host.isEmpty() || username == null || username.isEmpty()) {
            throw new IllegalStateException("SMTP 未配置（wxyd.smtp.host/username/password）");
        }
        Properties p = new Properties();
        p.put("mail.smtp.host", host);
        p.put("mail.smtp.port", String.valueOf(port));
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "15000");
        p.put("mail.smtp.ssl.enable", String.valueOf(port == 465));
        p.put("mail.smtp.starttls.enable", String.valueOf(port != 465));

        Session session = Session.getInstance(p, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            MimeMessage msg = new MimeMessage(session);
            String fromAddr = (from == null || from.isEmpty()) ? username : from;
            msg.setFrom(new InternetAddress(fromAddr, "小泽助手", "UTF-8"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            msg.setSubject(subject, "UTF-8");
            if (html) {
                msg.setContent(content, "text/html;charset=UTF-8");
            } else {
                msg.setText(content, "UTF-8");
            }
            msg.setSentDate(new Date());
            Transport.send(msg);
            log.info("邮件已发送：to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败：to={} err={}", to, e.getMessage());
            throw new IllegalStateException("邮件发送失败，请稍后重试");
        }
    }
}
