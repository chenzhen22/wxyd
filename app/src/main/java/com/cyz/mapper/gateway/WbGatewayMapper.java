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

    /** 网关鉴权：按 SM3 摘要查 Key（不筛启用状态，由服务层判定当天是否有效） */
    WbGatewayKey queryKeyByHash(@Param("keyHash") String keyHash);

    int deleteKey(@Param("id") Long id, @Param("userId") Long userId);

    int updateKeyEnabled(@Param("id") Long id, @Param("userId") Long userId, @Param("enabled") Integer enabled);

    int updateKeyModel(@Param("id") Long id, @Param("userId") Long userId, @Param("model") String model);

    /** 自动停用：生效日期不是今天的 Key 统一置为停用（次日生效） */
    int expireInactiveKeys();

    int touchKeyLastUsed(@Param("id") Long id);

    /** 模型开关：1=开（用户自选模型生效） 0=关（静默映射免费模型） */
    Integer queryModelSwitch();

    int updateModelSwitch(@Param("modelSwitch") Integer modelSwitch);

    /** 免费模型手动优先级（逗号分隔的模型 id，空=自动） */
    String queryFreePriority();

    int updateFreePriority(@Param("freePriority") String freePriority);

    /** 兜底模型（逗号分隔的模型 id，免费模型都不可用时的最后手段，按顺序） */
    String queryFallbackModels();

    int updateFallbackModels(@Param("fallbackModels") String fallbackModels);

    /** Qoder 每日自动签到开关（1=开 0=关） */
    Integer queryQoderAutoCheckin();

    int updateQoderAutoCheckin(@Param("enabled") Integer enabled);

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

    List<Map<String, Object>> usageLogs(@Param("userId") Long userId, @Param("date") String date,
                                        @Param("offset") int offset, @Param("limit") int limit);

    int usageLogsCount(@Param("userId") Long userId, @Param("date") String date);

    /** 清理超过保留期的用量记录 */
    int deleteUsageExpired(@Param("days") int days);

    /** 单条用量详情（含请求/应答数据） */
    Map<String, Object> usageDetail(@Param("id") Long id);
}
