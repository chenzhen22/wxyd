package com.chenzhen.controller;

import com.chenzhen.pojo.Result;
import com.chenzhen.service.ManifestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ManifestController implements CommController{

    @Autowired
    ManifestService manifestService;

    @ResponseBody
    @RequestMapping("orderCreate")
    public Result orderCreate(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String orderO = (String) map.get("orderO");
        List<String> orderList = manifestService.getOrderList(orderO);
        result = Result.getInstance();
        result.setBody(orderList);
        return result;
    }

    @ResponseBody
    @RequestMapping("datadict")
    public Result datadict(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String orderO = (String) map.get("orderO");
        List<String> orderList = manifestService.datadict(orderO);
        result = Result.getInstance();
        result.setBody(orderList);
        return result;
    }

    @ResponseBody
    @RequestMapping("requestData")
    public Result requestData(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String orderO = (String) map.get("orderO");
        List<String> orderList = manifestService.requestData(orderO);
        result = Result.getInstance();
        result.setBody(orderList);
        return result;
    }

    @ResponseBody
    @RequestMapping("responseData")
    public Result responseData(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String orderO = (String) map.get("orderO");
        List<String> orderList = manifestService.responseData(orderO);
        result = Result.getInstance();
        result.setBody(orderList);
        return result;
    }
}
