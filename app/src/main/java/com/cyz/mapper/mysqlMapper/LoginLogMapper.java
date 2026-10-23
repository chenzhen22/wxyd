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
     * 分页查询：在过滤条件下（userId 非空只查该用户；event 非空只查该事件类型；
     * usernameLike 非空按用户名/邮箱模糊匹配，仅管理员使用）按 id 倒序返回
     * [offset, offset+limit) 区间。
     */
    List<LoginLog> list(@Param("userId") Long userId, @Param("event") String event,
                        @Param("usernameLike") String usernameLike,
                        @Param("offset") int offset, @Param("limit") int limit);

    /** 统计符合条件的记录数（分页总数）。 */
    int count(@Param("userId") Long userId, @Param("event") String event,
              @Param("usernameLike") String usernameLike);

    /** 清理保留期之前的日志 */
    int deleteOlderThanDays(@Param("days") int days);
}
