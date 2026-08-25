package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@Slf4j
public class MessageController implements CommController {

    @Autowired
    MessageService messageService;

    @ResponseBody
    @RequestMapping("queryMessage")
    public Result queryMessage(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String flag = map.get("flag") == null ? "" : String.valueOf(map.get("flag"));
        Long userId = (Long) session.getAttribute("userId");
        return messageService.queryMessage(userId, flag);
    }

    @ResponseBody
    @RequestMapping("addMessage")
    public Result addMessage(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String message = (String) map.get("message");
        Long userId = (Long) session.getAttribute("userId");
        return messageService.addMessage(userId, message);
    }

    @ResponseBody
    @RequestMapping("delMessage")
    public Result delMessage(@RequestBody Result result, HttpSession session) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String msgId = String.valueOf(map.get("msgId"));
        Long userId = (Long) session.getAttribute("userId");
        Integer role = (Integer) session.getAttribute("role");
        // 管理员（role=0）可删除任意留言
        if (role != null && role == 0) {
            return messageService.delMessage(msgId);
        }
        // 普通用户只能删除自己的（按 user_id 校验归属）
        int n = messageService.delMessageByUser(userId, msgId);
        if (n == 0) {
            Result r = Result.getInstance();
            r.setErrorCode("000003");
            r.setErrorMsg("只能删除自己的留言");
            return r;
        }
        return Result.getInstance();
    }

}
