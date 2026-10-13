package com.cyz.mapper.mysqlMapper;

import com.cyz.pojo.LoginLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 登录日志 mapper（login_log 表，保留 90 天由定时任务清理）。
 */
public interface LoginLogMapper {

    int insert(LoginLog log);

    /**
     * 最新 200 条：userId 非空则只查该用户；usernameLike 非空则按用户名/邮箱模糊匹配（仅管理员使用）。
     */
    List<LoginLog> list(@Param("userId") Long userId, @Param("usernameLike") String usernameLike);

    /** 清理保留期之前的日志 */
    int deleteOlderThanDays(@Param("days") int days);
}
