package com.cyz.mapper.mysqlMapper;

import com.cyz.radar.model.RadarSubscribe;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 羊毛雷达订阅 mapper（radar_subscribe 表）。
 */
public interface RadarMapper {

    List<RadarSubscribe> listByUser(@Param("userId") Long userId);

    RadarSubscribe getById(@Param("id") Long id);

    int insert(RadarSubscribe s);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    int updateEnabled(@Param("id") Long id, @Param("userId") Long userId, @Param("enabled") Integer enabled);

    int countByUser(@Param("userId") Long userId);

    int updateLastPostId(@Param("id") Long id, @Param("lastPostId") Long lastPostId);

    /** 全部启用中的订阅（定时任务用） */
    List<RadarSubscribe> listEnabled();

    /** scan_post 当前最大 id（新订阅以此为初始水位，只推未来新帖） */
    Long selectMaxPostId();

    /** 增量取命中帖：id > 水位 且 标题/摘要含关键词（远程库 only_full_group_by 已注意，无聚合） */
    List<Map<String, Object>> findHits(@Param("keyword") String keyword,
                                       @Param("lastPostId") Long lastPostId,
                                       @Param("limit") Integer limit);
}
