package com.chenzhen.config;

import com.chenzhen.interceptor.FeignInterceptor;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeginConfig {

    @Bean
    public RequestInterceptor feignInterceptor() {
        return new FeignInterceptor();
    }
}
