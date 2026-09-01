package com.cyz.dubbo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Dubbo 泛化调用配置（前端表单 / localStorage 存储单元）。
 * <p>移植自 do_dubbo，字段含义保持一致。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DubboConfig {

    private String id;              // UUID，前端 localStorage 主键
    private String name;            // 展示名，可重命名
    private String registryAddress; // 注册中心地址，如 zookeeper://127.0.0.1:2181
    private String serviceName;     // 服务名 → Dubbo group，如 dubbov_CPR020181Flow
    private String version = "1.0.0";
    private String interfaceName = "com.ifp.core.flow.service.FlowService";
    private String methodName = "execute";
    private String paramJson;       // 请求参数 JSON（顶层 reqId + dataMap）
    private Integer timeout = 30000;
    private String createTime;
    private String updateTime;
}
