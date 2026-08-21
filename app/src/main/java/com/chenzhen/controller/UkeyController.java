package com.chenzhen.controller;

import com.chenzhen.pojo.Result;
import com.chenzhen.service.UkeyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@Slf4j
public class UkeyController implements CommController{

    @Autowired
    UkeyService ukeyService;

    @ResponseBody
    @RequestMapping("queryUdInfo")
    public Result queryUdInfo(@RequestBody Result result) {
        return ukeyService.queryUdInfo();
    }

    @ResponseBody
    @RequestMapping(value = "/queryCprUser")
    public Result queryCprUser(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String userId = (String) map.get("userId");
        String usbkey = (String) map.get("usbkey");
        String Ostype = (String) map.get("Ostype");
        return ukeyService.updateUK(userId, Ostype, usbkey);
    }

    @ResponseBody
    @RequestMapping("udOper")
    public Result udOper(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String udStatue = (String) map.get("udStatue");
        String udhost = (String) map.get("udhost");
        return ukeyService.udOper(udStatue, udhost);
    }

    @ResponseBody
    @RequestMapping("unBindUkey")
    public Result unBindUkey(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String zsNumber = (String) map.get("zsNumber");
        return ukeyService.unBindUkey(zsNumber);
    }

    @ResponseBody
    @RequestMapping("cfcaInfoQry")
    public Result cfcaInfoQry(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String zsNumber = (String) map.get("zsNumber");
        return ukeyService.cfcaInfoQry(zsNumber);
    }
}

