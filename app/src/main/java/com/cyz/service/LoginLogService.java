package com.cyz.service;

import com.cyz.mapper.mysqlMapper.LoginLogMapper;
import com.cyz.pojo.LoginLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 登录日志服务。
 * <p>
 * record 为尽力而为：任何异常只打日志、不上抛，登录主流程不因日志失败受影响。
 * 日志保留 90 天，每日 4 点定时清理（{@link #cleanExpired}）。
 */
@Service
@Slf4j
public class LoginLogService {

    private static final int RETAIN_DAYS = 90;

    @Resource
    private LoginLogMapper loginLogMapper;

    /**
     * 记录一条登录相关事件（登录成功/失败/登出），失败不影响主流程。
     */
    public void record(Long userId, String username, String event, String channel, String ip, String detail) {
        try {
            LoginLog log = new LoginLog();
            log.setUserId(userId);
            log.setUsername(username == null ? "" : username);
            log.setEvent(event);
            log.setChannel(channel);
            log.setIp(ip);
            log.setDetail(detail);
            loginLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("登录日志记录失败：username={} event={} err={}", username, event, e.getMessage());
        }
    }

    /**
     * 查询最新 200 条：userId 非空只查该用户；event 非空只查该事件类型；usernameLike 非空按用户名模糊过滤。
     */
    public List<LoginLog> list(Long userId, String event, String usernameLike) {
        return loginLogMapper.list(userId, event, usernameLike);
    }

    /** 每日 4 点清理超过保留期的登录日志 */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanExpired() {
        try {
            int deleted = loginLogMapper.deleteOlderThanDays(RETAIN_DAYS);
            if (deleted > 0) {
                log.info("登录日志定时清理：删除 {} 条超过 {} 天的记录", deleted, RETAIN_DAYS);
            }
        } catch (Exception e) {
            log.warn("登录日志定时清理失败：{}", e.getMessage());
        }
    }
}
