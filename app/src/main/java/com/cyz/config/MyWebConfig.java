package com.cyz.config;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

//@Configuration
public class MyWebConfig extends WebMvcConfigurationSupport {

    @Override
    public void addResourceHandlers (ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/Assets/css/**").addResourceLocations("classpath:/static/Assets/css/");
        registry.addResourceHandler("/Assets/images/**").addResourceLocations("classpath:/static/Assets/images/");
        registry.addResourceHandler("/Assets/js/**").addResourceLocations("classpath:/static/Assets/js/");
        registry.addResourceHandler("/Assets/plugins/**").addResourceLocations("classpath:/static/Assets/plugins/");
        registry.addResourceHandler("/Assets/upload/**").addResourceLocations("classpath:/static/Assets/upload/");
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }
}
