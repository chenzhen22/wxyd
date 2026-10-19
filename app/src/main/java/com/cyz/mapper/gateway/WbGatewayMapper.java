package com.cyz.mapper.gateway;

import com.cyz.pojo.WbGatewayKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** AI 工具模型网关 Mapper（wb_gateway_key / wb_gateway_config）。 */
public interface WbGatewayMapper {

    int countByUserId(@Param("userId") Long userId);

    int insertKey(WbGatewayKey key);

    List<WbGatewayKey> listByUserId(@Param("userId") Long userId);

    WbGatewayKey queryKeyById(@Param("id") Long id, @Param("userId") Long userId);

    /** 网关鉴权：按 SM3 摘要查启用中的 Key */
    WbGatewayKey queryKeyByHash(@Param("keyHash") String keyHash);

    int deleteKey(@Param("id") Long id, @Param("userId") Long userId);

    int updateKeyEnabled(@Param("id") Long id, @Param("userId") Long userId, @Param("enabled") Integer enabled);

    int updateKeyModel(@Param("id") Long id, @Param("userId") Long userId, @Param("model") String model);

    int touchKeyLastUsed(@Param("id") Long id);

    /** 模型开关：1=开（用户自选模型生效） 0=关（静默映射免费模型） */
    Integer queryModelSwitch();

    int updateModelSwitch(@Param("modelSwitch") Integer modelSwitch);

    /** 管理员：全部用户的 Key 列表（join users 带属主名） */
    List<Map<String, Object>> adminListKeys();

    WbGatewayKey adminQueryKeyById(@Param("id") Long id);

    /** 管理员：设置单条 Key 的模型开关（null=跟随全局） */
    int adminUpdateKeyModelOverride(@Param("id") Long id, @Param("modelOverride") Integer modelOverride);

    /* ===== 令牌消耗统计 ===== */

    int insertUsage(com.cyz.pojo.WbGatewayUsage usage);

    Map<String, Object> usageTotals();

    Map<String, Object> usageTotalsToday();

    List<Map<String, Object>> usageByUser();

    List<Map<String, Object>> usageByModel();

    List<Map<String, Object>> usageLogs(@Param("offset") int offset, @Param("limit") int limit);

    int usageLogsCount();
}
