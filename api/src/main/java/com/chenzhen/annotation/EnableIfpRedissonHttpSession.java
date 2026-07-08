package com.chenzhen.annotation;

import com.chenzhen.config.IfpRedissonSessionConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import({IfpRedissonSessionConfiguration.class})
@Configuration
public @interface EnableIfpRedissonHttpSession {

    int maxInactiveIntervalInSeconds() default 1800;

    String keyPrefix() default "";
}
