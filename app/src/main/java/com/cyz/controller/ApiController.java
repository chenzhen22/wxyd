package com.cyz.controller;

import com.cyz.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
public class ApiController implements CommController{

    @Autowired
    ApiService apiService;

    @ResponseBody
    @RequestMapping("tbphx.do")
    public Object tbphx(@RequestParam(value = "transData", required = false) String transData, HttpServletRequest request) throws Exception {
        return apiService.tbphx(transData, request);
    }
}
