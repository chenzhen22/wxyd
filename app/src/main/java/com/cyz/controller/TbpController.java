package com.chenzhen.controller;

import com.chenzhen.pojo.Result;
import com.chenzhen.service.TbpService;
import com.chenzhen.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
public class TbpController implements CommController{

    @Autowired
    TbpService tbpService;

    @GetMapping("/dec")
    public Result dec(@Param("type") String type, @Param("password") String password) {
        return tbpService.dec(type, password);
    }

    @RequestMapping("/getParamValue")
    public Result getParamValue(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String key = (String) map.get("key");
        String value = CommUtils.getParamValue(key);
        Result res = Result.getInstance();
        res.setBody(value);
        return res;
    }
}
