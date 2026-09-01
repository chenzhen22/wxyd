package com.cyz.dubbo.service;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cyz.dubbo.model.DubboConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dubbo 泛化调用器：按配置动态引用服务并 $invoke，调用完即销毁 ReferenceConfig，
 * 避免长连接堆积。移植自 do_dubbo。
 */
@Component
public class DubboInvoker {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final ApplicationConfig applicationConfig;

    public DubboInvoker() {
        applicationConfig = new ApplicationConfig("wxyd-dubbo-tool");
        applicationConfig.setLogger("slf4j");   // 与 Spring Boot 日志栈一致
    }

    public Object invoke(DubboConfig config, String clientIp) {
        Map<String, Object> paramMap = prepareParamMap(config, clientIp);

        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        reference.setApplication(applicationConfig);
        RegistryConfig registry = new RegistryConfig(config.getRegistryAddress());
        registry.setTransporter("curator");   // 默认 zkclient 传输需 zkclient 库（Nexus 无）；curator 由 dubbov 传递引入
        reference.setRegistry(registry);
        reference.setInterface(config.getInterfaceName());
        reference.setGeneric(true);
        reference.setGroup(config.getServiceName());
        reference.setVersion(config.getVersion());
        reference.setTimeout(config.getTimeout() == null ? 30000 : config.getTimeout());
        try {
            GenericService genericService = reference.get();
            return genericService.$invoke(
                    config.getMethodName(),
                    new String[]{"java.util.Map"},
                    new Object[]{paramMap});
        } finally {
            reference.destroy();
        }
    }

    /**
     * 解析 paramJson 为 Map，并把 reqId 占位符 {@link ReqIdGenerator#PLACEHOLDER}
     * 替换为后端按规则生成的实际 reqId。供 {@link #invoke} 与测试使用。
     *
     * @param config   请求配置
     * @param clientIp 客户端 ip，用于生成 reqId
     * @return 可供泛化调用的参数 Map
     */
    public Map<String, Object> prepareParamMap(DubboConfig config, String clientIp) {
        Map<String, Object> paramMap = parseParamJson(config.getParamJson());
        ReqIdGenerator.resolvePlaceholder(paramMap, clientIp);
        return paramMap;
    }

    static Map<String, Object> parseParamJson(String json) {
        try {
            return mapper.readValue(json,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("参数 JSON 解析失败: " + e.getMessage(), e);
        }
    }
}
