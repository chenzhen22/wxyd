package com.cyz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Properties;

/**
 * Formerly read configuration from Nacos (data-id "common").
 * With Nacos removed, reads from a local classpath file "common.properties"
 * (the migrated content of the former Nacos "common" config). Mock mode does
 * not call getMapperValue; callers (CommUtils, MailUtil) are dormant under mock.
 */
@Component
@Slf4j
public class ProperConfig {

    private static final String COMMON_RESOURCE = "common.properties";

    private static final Properties props = new Properties();

    @PostConstruct
    public void init() {
        Resource resource = new ClassPathResource(COMMON_RESOURCE);
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                props.load(is);
                log.info("ProperConfig loaded {} ({} keys)", COMMON_RESOURCE, props.size());
            } catch (Exception e) {
                log.error("ProperConfig failed to load {}: {}", COMMON_RESOURCE, e.getMessage());
            }
        } else {
            log.warn("ProperConfig: {} not found on classpath; getMapperValue will return null", COMMON_RESOURCE);
        }
    }

    public static String getMapperValue(String key) {
        return props.getProperty(key);
    }
}
