package com.cyz.mapper.mysqlMapper;

import com.cyz.patrol.model.PatrolAsset;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 巡检资产/设置 mapper（patrol_asset / patrol_setting 表）。
 */
public interface PatrolMapper {

    List<PatrolAsset> listByUser(@Param("userId") Long userId);

    List<PatrolAsset> listEnabledByUser(@Param("userId") Long userId);

    PatrolAsset getById(@Param("id") Long id);

    int insert(PatrolAsset a);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    int updateCheckResult(@Param("id") Long id, @Param("status") String status,
                          @Param("detail") String detail);

    /* ---- 日报设置 ---- */

    Map<String, Object> getSetting(@Param("userId") Long userId);

    int upsertSetting(@Param("userId") Long userId, @Param("pushHour") Integer pushHour,
                      @Param("channel") String channel, @Param("robotId") Long robotId,
                      @Param("email") String email, @Param("enabled") Integer enabled);

    /** 需要在当前小时推日报的用户（enabled=1 且 push_hour 匹配且已配置通道） */
    List<Map<String, Object>> listUsersToPush(@Param("hour") Integer hour);
}
