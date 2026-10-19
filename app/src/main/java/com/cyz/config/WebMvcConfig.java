package com.cyz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private com.cyz.filter.DoSuffixFilter doSuffixFilter;

    /** dataScan 图片落地目录（福利专区 /welfare-img/** 静态映射） */
    @Value("${wxyd.welfare.image-dir:/apps/shareFile/datascan}")
    private String welfareImageDir;

    /** .do 后缀过滤器注册为最高优先级，先于一切组件剥掉 .do 再进入 MVC */
    @Bean
    public FilterRegistrationBean<com.cyz.filter.DoSuffixFilter> doSuffixFilterRegistration() {
        FilterRegistrationBean<com.cyz.filter.DoSuffixFilter> reg = new FilterRegistrationBean<>(doSuffixFilter);
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.setName("doSuffixFilter");
        return reg;
    }

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
                        "/radar/**",
                        "/patrol/**",
                        "/dayinfo/**",
                        "/gateway/**",
                        "/loginLog/**",
                        "/approveUser", "/rejectUser", "/deleteUser",
                        "/queryMessage", "/addMessage", "/delMessage"
                );
        // 注意：/welfare/** 与 /welfare-img/** 不在拦截范围内——福利专区为公开只读
        // 数据（分享链接免登录查看），不含任何用户数据。
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 福利专区图片：dataScan 已把正文图片下载到本地（按 topic_id 建子目录）。
        // 公开访问（分享链接免登录），仅暴露该目录内容；
        // ResourceHttpRequestHandler 会规范化路径并拒绝 ../ 越界，不会读到目录之外。
        String dir = welfareImageDir == null || welfareImageDir.trim().isEmpty()
                ? "/apps/shareFile/datascan" : welfareImageDir.trim();
        if (!dir.endsWith("/")) dir = dir + "/";
        registry.addResourceHandler("/welfare-img/**")
                .addResourceLocations("file:" + dir);
    }
}
