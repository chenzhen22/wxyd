package com.chenzhen.config;

import com.chenzhen.annotation.EnableIfpRedissonHttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

public class IfpRedissonSessionConfiguration implements ImportAware {

    @Value("${ifp.session.prefix:}")
    private String sessionKeyPrefix;
    private Integer maxInactiveIntervalInSeconds;
    private String keyPrefix;

    @Bean
    public IfpRedissonSessionConfiguration.IfpRedissonSessionRepository sessionRepository(ApplicationEventPublisher eventPublisher) {
        IfpRedissonSessionConfiguration.IfpRedissonSessionRepository repository = new IfpRedissonSessionConfiguration.IfpRedissonSessionRepository();
        return repository;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableIfpRedissonHttpSession.class.getName());
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(map);
        this.keyPrefix = attrs.getString("keyPrefix");
        this.maxInactiveIntervalInSeconds = (Integer)attrs.getNumber("maxInactiveIntervalInSeconds");
    }
    public static class IfpRedissonSessionRepository{

    }
}
