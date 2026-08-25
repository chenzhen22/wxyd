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
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/register", "/login.html", "/register.html",
                        "/**/*.html", "/**/*.js", "/**/*.css", "/**/*.png",
                        "/**/*.jpg", "/**/*.gif", "/**/*.svg", "/**/*.ico",
                        "/**/*.woff", "/**/*.woff2", "/**/*.ttf",
                        "/", "/actuator/**", "/error"
                );
    }
}
