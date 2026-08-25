package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.service.LoginService;
import com.cyz.util.CommUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class LoginController implements CommController{

    @Autowired
    LoginService loginService;

    @ResponseBody
    @RequestMapping("mmLogin")
    public Result mmLogin(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String userName = (String) map.get("userName");
        String Ostype = (String) map.get("Ostype");
        return loginService.updateToken(userName, Ostype);
    }

    @ResponseBody
    @RequestMapping("ngLogin")
    public Result ngLogin(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String userName = (String) map.get("userName");
        String Ostype = (String) map.get("Ostype");
        String lstype = (String) map.get("lstype");
        String clientIp = CommUtils.getClientIpByMDC();
        return loginService.updateTokenNG(userName, Ostype, lstype, clientIp);
    }
}
