package com.chenzhen.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Configuration
@Slf4j
public class ProperConfig {

    public static final String DATA_ID="common";
    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    @Value("${spring.cloud.nacos.discovery.server-addr:}")
    private String serverAddr;

    @Value("${spring.cloud.nacos.config.namespace:}")
    private String namespace;

    @Value("${spring.cloud.nacos.discovery.username:}")
    private String username;

    @Value("${spring.cloud.nacos.discovery.password:}")
    private String password;

    private static ConfigService configService;

    @PostConstruct
    public void init() {
        try {
            Properties props = new Properties();
            props.put("serverAddr", serverAddr);
            props.put("namespace", namespace);
            props.put("username", username);
            props.put("password", password);
            configService = NacosFactory.createConfigService(props);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    public static String getMapperValue(String key) {
        try {
            String value = configService.getConfig(DATA_ID,DEFAULT_GROUP,5000);
            String[] values = value.split("\r\n", -1);
            for(String val : values) {
                String[] vals = val.split("=", -1);
                if(key.equals(vals[0])) {
                    return vals[1];
                }
            }
        } catch (NacosException e) {
            e.printStackTrace();
        }
        return null;
    }

}
