package com.cyz.mapper.mysqlMapper;

import com.cyz.pojo.DayInfo;
import com.cyz.pojo.DayInfoSubscribe;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 金融日报 mapper（day_info / day_info_subscribe 表）。
 */
public interface DayInfoMapper {

    /** 按类型+日期取唯一一条（upsert 判存用） */
    DayInfo getByTypeAndDate(@Param("type") String type, @Param("infoDate") String infoDate);

    int insert(DayInfo info);

    int updateContent(DayInfo info);

    /** 按日期倒序分页 */
    List<DayInfo> listByType(@Param("type") String type,
                             @Param("offset") Integer offset,
                             @Param("pageSize") Integer pageSize);

    int countByType(@Param("type") String type);

    DayInfo getById(@Param("id") Long id);

    DayInfo getLatest(@Param("type") String type);

    /* ---------------- 订阅（一人多条：多个钉钉机器人 + 多个邮箱） ---------------- */

    List<DayInfoSubscribe> listByUser(@Param("userId") Long userId);

    DayInfoSubscribe getSubById(@Param("id") Long id);

    int insertSubscribe(DayInfoSubscribe s);

    /** 双重限定：id 归属校验在 SQL 层兜底 */
    int deleteSubscribe(@Param("id") Long id, @Param("userId") Long userId);

    int updateSubscribeEnabled(@Param("id") Long id, @Param("userId") Long userId, @Param("enabled") Integer enabled);

    int countByUser(@Param("userId") Long userId);

    /** 全部启用中的订阅（新日报入库提醒用） */
    List<DayInfoSubscribe> listEnabledSubscribe();
}
