package com.cyz.pojo;

import lombok.Data;

/**
 * AI 工具模型网关的 API Key（wb_gateway_key）。
 * <p>
 * api_key 列存 SM4 密文（随机 IV，不可等值查询）；等值查询走 key_hash（SM3 明文摘要）。
 * model 为用户在页面上自选的模型（取自 WorkBuddy 实时目录），仅模型开关开启时生效。
 */
@Data
public class WbGatewayKey {

    /** 每用户最多持有的 Key 数量 */
    public static final int MAX_KEYS_PER_USER = 5;

    private Long id;
    private Long userId;
    private String name;
    /** SM4 密文 */
    private String apiKey;
    /** Key 明文的 SM3 摘要（64 hex） */
    private String keyHash;
    /** 用户自选模型，空=按请求模型/网关默认 */
    private String model;
    /** Key 级模型开关：NULL=跟随全局 1=强制开 0=强制关（仅管理员可设） */
    private Integer modelOverride;
    private Integer enabled;
    /** 生效日期（yyyy-MM-dd）：仅当天启用有效，次日视为过期需用户重新启用 */
    private String activeDate;
    /** Key 属主角色（0=管理员）：管理员 Key 豁免每日停用 */
    private Integer userRole;
    private String createTime;
    private String lastUsedTime;
}
