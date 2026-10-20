package com.cyz.mapper.mysqlMapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 巡检日报 mapper（patrol_setting 表；巡检结果不落库，存内存）。
 */
public interface PatrolMapper {

    /** 校验钉钉机器人归属（日报设置绑定的机器人是否仍有效） */
    int countRobot(@Param("userId") Long userId, @Param("robotId") Long robotId);

    /* ---- 日报设置 ---- */

    Map<String, Object> getSetting(@Param("userId") Long userId);

    int upsertSetting(@Param("userId") Long userId, @Param("pushHour") Integer pushHour,
                      @Param("channel") String channel, @Param("robotId") Long robotId,
                      @Param("email") String email, @Param("enabled") Integer enabled);

    /** 需要在当前小时推日报的用户（enabled=1 且 push_hour 匹配且已配置通道） */
    List<Map<String, Object>> listUsersToPush(@Param("hour") Integer hour);
}
