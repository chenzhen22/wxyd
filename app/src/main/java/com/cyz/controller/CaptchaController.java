package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.service.SliderCaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * 滑块验证码：公开接口（不在 AuthInterceptor 拦截黑名单内），
 * 供邮箱注册流程在发送验证码邮件前做人机校验。
 */
@RestController
@Slf4j
public class CaptchaController implements CommController {

    @Autowired
    private SliderCaptchaService sliderCaptchaService;

    @RequestMapping("captcha/slider")
    public Result slider() {
        Result result = Result.getInstance();
        try {
            result.setBody(sliderCaptchaService.generate());
        } catch (Exception e) {
            log.error("生成滑块验证码失败", e);
            result.setErrorCode("000003");
            result.setErrorMsg("验证码生成失败，请重试");
        }
        return result;
    }

    @RequestMapping("captcha/verify")
    public Result verify(@RequestBody Result req) {
        Map<String, Object> map = (Map<String, Object>) req.getBody();
        String captchaId = (String) map.get("captchaId");
        Object xObj = map.get("x");
        Result result = Result.getInstance();
        try {
            if (captchaId == null || xObj == null) {
                throw new IllegalArgumentException("参数缺失");
            }
            String ticket = sliderCaptchaService.verify(captchaId, Long.parseLong(String.valueOf(xObj)));
            result.setBody(Collections.singletonMap("ticket", ticket));
        } catch (IllegalArgumentException e) {
            result.setErrorCode("000003");
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }
}
