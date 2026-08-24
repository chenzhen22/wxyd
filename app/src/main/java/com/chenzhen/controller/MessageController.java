package com.chenzhen.controller;

import com.chenzhen.pojo.Result;
import com.chenzhen.service.MessageService;
import com.chenzhen.util.CommUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class MessageController implements CommController{

    @Autowired
    MessageService messageService;

    @ResponseBody
    @RequestMapping("addMessage")
    public Result addMessage(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String message = (String) map.get("message");
        return messageService.addMessage(message);
    }

    @ResponseBody
    @RequestMapping("delMessage")
    public Result delMessage(@RequestBody Result result) {
        Map<String, Object> resultMap = (Map<String, Object>) result.getBody();
        String msgId = (String) resultMap.get("msgId");

        String client = CommUtils.getClientIpByMDC();
        String adminIp = CommUtils.getParamValue("mailIp");
        if(client.equals(adminIp)) {
            return messageService.delMessage(msgId);
        }
        Result msgRs = messageService.queryMessageById(msgId, "");
        Object obj = msgRs.getBody();
        result = Result.getInstance();
        if(null != obj) {
            Map<String, Object> resMap = (Map<String, Object>) obj;
            List<Map<String, Object>> resList = (List<Map<String, Object>>) resMap.get("msgList");
            if(resList.isEmpty()) {
                return result;
            }
            Map<String, Object> map = resList.get(0);
            if(!client.equals(map.get("ip"))) {
                result.setErrorCode("000003");
                result.setErrorMsg("只能删除自己的留言");
                return result;
            }
        }
        return messageService.delMessage(msgId);
    }

    @ResponseBody
    @RequestMapping("queryMessage")
    public Result queryMessage(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String flag = (String) map.get("flag");
        return messageService.queryMessage(flag);
    }

}
