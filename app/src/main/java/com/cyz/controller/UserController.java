package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.pojo.WhiteUser;
import com.cyz.service.UserService;
import com.cyz.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class UserController implements CommController{

    @Autowired
    UserService userService;

    @ResponseBody
    @RequestMapping("queryWhiteInfo")
    public Result queryWhiteInfo(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String userName = (String) map.get("userName");
        WhiteUser whiteUser = new WhiteUser();
        whiteUser.setUserName(userName);
        List<WhiteUser> user = userService.queryWhiteInfo(whiteUser);
        result = Result.getInstance();
        result.setBody(user);
        return result;
    }

    @ResponseBody
    @RequestMapping("addWhite")
    public Result addWhite(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String whitename = (String) map.get("whitename");

        String client = CommUtils.getClientIpByMDC();
        WhiteUser user = new WhiteUser();
        user.setIp(client);
        user.setStatus("1");

        List<WhiteUser> list = userService.queryWhiteInfo(user);
        user.setUserName(whitename);
        result = Result.getInstance();
        if(list.size() > 0 ) {
            WhiteUser wu = list.get(0);
            if("0".equals(wu.getStatus())) {
                result.setErrorCode("000001");
                result.setErrorMsg("已是白名单用户");
                return result;
            } else {
                int index = userService.updateWhiteByIp(user);
                result = CommUtils.handleDaoResult(index);
                return result;
            }
        }

        int index = userService.addWhite(user);
        result = CommUtils.handleDaoResult(index);
        return result;
    }

    @ResponseBody
    @RequestMapping("updateWhite")
    public Result updateWhite(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String whitename = (String) map.get("whitename");

        String client = CommUtils.getClientIpByMDC();
        String adminIp = CommUtils.getParamValue("mailIp");
        result = Result.getInstance();
        if(!client.equals(adminIp)) {
            result.setErrorCode("000003");
            result.setErrorMsg("权限不足，请联系管理员");
            return result;
        }

        WhiteUser user = new WhiteUser();
        user.setStatus("0");
        user.setUserName(whitename);

        int index = userService.updateWhiteByUsername(user);
        result = CommUtils.handleDaoResult(index);
        return result;
    }

    @ResponseBody
    @RequestMapping("deleteWhite")
    public Result deleteWhite(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String whitename = (String) map.get("whitename");

        String client = CommUtils.getClientIpByMDC();
        String adminIp = CommUtils.getParamValue("mailIp");
        result = Result.getInstance();
        if(!client.equals(adminIp)) {
            result.setErrorCode("000003");
            result.setErrorMsg("权限不足，请联系管理员");
            return result;
        }

        WhiteUser user = new WhiteUser();
        user.setStatus("0");
        user.setUserName(whitename);

        int index = userService.deleteWhite(user);
        result = CommUtils.handleDaoResult(index);
        return result;
    }


}
