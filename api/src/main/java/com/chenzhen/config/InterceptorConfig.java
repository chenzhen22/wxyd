package com.chenzhen.config;

import com.chenzhen.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@Slf4j
public class InterceptorConfig implements WebMvcConfigurer {


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> arg0) {

    }

    @Override
    public void addCorsMappings(CorsRegistry arg0) {

    }

    @Override
    public void addFormatters(FormatterRegistry arg0) {

    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {

            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object arg2) {
                String traceId = LogUtil.getTraceId();
                MDC.put("traceId", traceId);
                return true;
            }

            @Override
            public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
                    throws Exception {
            }

            @Override
            public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
                    throws Exception {
            }
        });
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry arg0) {

    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> arg0) {

    }

    @Override
    public void addViewControllers(ViewControllerRegistry arg0) {

    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer arg0) {

    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer arg0) {

    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configurePathMatch(PathMatchConfigurer arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public MessageCodesResolver getMessageCodesResolver() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Validator getValidator() {
        // TODO Auto-generated method stub
        return null;
    }


}
