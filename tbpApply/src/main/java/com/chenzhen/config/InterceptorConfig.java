package com.chenzhen.config;

import cn.hutool.core.stream.StreamUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.constant.ErrorEnum;
import com.chenzhen.pojo.LogPojo;
import com.chenzhen.pojo.Result;
import com.chenzhen.request.RepeatReadRequestWrapper;
import com.chenzhen.service.ClientInfo;
import com.chenzhen.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    ClientInfo clientInfo;

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
                try {
                    String clientIp = CommUtils.getClientIp(request);
                    try {
                        RepeatReadRequestWrapper wrapper = (RepeatReadRequestWrapper) request;
                        String reqStr = wrapper.getBody();
                        JSONObject jsonObject = JSON.parseObject(reqStr);
                        clientIp = jsonObject.getString("clientIp");
                    } catch (Exception e) {}

                    String url = request.getRequestURI();
                    if (url.endsWith("html") || url.endsWith("css") || url.endsWith("js") || url.endsWith("jpg") || url.endsWith("png") || url.endsWith("error")) {
                        return true;
                    }
                    url = url.substring(url.lastIndexOf("/") + 1);
                    if (!"queryMessage".equals(url)) {
                        LogPojo logPojo = new LogPojo();
                        logPojo.setClientIp(clientIp);
                        logPojo.setUrl(url);
                        Map map = request.getParameterMap();
                        logPojo.setData(JSON.toJSONString(map));
                        clientInfo.addLog(logPojo);
                        log.info("access:{}", logPojo);
                    }

                    if (clientInfo.checkwhite(url)) {
                        return true;
                    }
                    Map<String, String> clientMap = clientInfo.getUserName(clientIp);

                    if (null != clientMap && "0".equals(clientMap.get("status"))) {
                        return true;
                    }
                    response.setCharacterEncoding("UTF-8");
                    response.setHeader("Content-type", "application/json");
                    Result result = Result.getInstance();
                    result.setErrorEnum(ErrorEnum.ERROR888888);
                    JSONObject json = (JSONObject) JSONObject.toJSON(result);
                    response.getOutputStream().write((json.toString()).getBytes());
                    return false;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

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
