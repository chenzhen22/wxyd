package com.cyz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 仅守护新增的管理端点；遗留外部入口（tbphx.do）、静态页、actuator 等不受影响，
        // 避免误拦截机器到机器的 transData 集成调用。
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/user/**", "/robot/**", "/javaapi/**", "/shellscript/**", "/note", "/note/**", "/dubbo/**",
                        "/archive/**",
                        "/approveUser", "/rejectUser", "/deleteUser",
                        "/queryMessage", "/addMessage", "/delMessage"
                );
    }
}
