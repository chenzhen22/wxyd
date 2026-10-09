package com.cyz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /** dataScan 图片落地目录（福利专区 /welfare-img/** 静态映射） */
    @Value("${wxyd.welfare.image-dir:/apps/shareFile/datascan}")
    private String welfareImageDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 仅守护新增的管理端点；遗留外部入口（tbphx.do）、静态页、actuator 等不受影响，
        // 避免误拦截机器到机器的 transData 集成调用。
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/user/**", "/robot/**", "/javaapi/**", "/shellscript/**", "/note", "/note/**", "/dubbo/**",
                        "/archive/**",
                        "/share/**",
                        "/sql/**",
                        "/group/**",
                        "/menu/**",
                        "/welfare/**",
                        "/welfare-img/**",
                        "/approveUser", "/rejectUser", "/deleteUser",
                        "/queryMessage", "/addMessage", "/delMessage"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 福利专区图片：dataScan 已把正文图片下载到本地（按 topic_id 建子目录），
        // 这里映射出去，需登录（见上方拦截器）。ResourceHttpRequestHandler 会规范化
        // 路径并拒绝 ../ 越界，不会读到映射目录之外。
        String dir = welfareImageDir == null || welfareImageDir.trim().isEmpty()
                ? "/apps/shareFile/datascan" : welfareImageDir.trim();
        if (!dir.endsWith("/")) dir = dir + "/";
        registry.addResourceHandler("/welfare-img/**")
                .addResourceLocations("file:" + dir);
    }
}
