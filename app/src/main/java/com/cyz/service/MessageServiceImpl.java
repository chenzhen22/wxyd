package com.chenzhen.service;

import cfca.yuzhi.vo.util.StringUtil;
import com.chenzhen.mapper.mysqlMapper.MysqlMapper;
import com.chenzhen.pojo.Messages;
import com.chenzhen.pojo.Result;
import com.chenzhen.util.CommUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    MysqlMapper mysqlMapper;

    @Autowired
    UserService userService;

    @Override
    public Result addMessage(String message) {
        String clientIp = CommUtils.getClientIpByMDC();
        Messages messages = new Messages(clientIp, message);
        mysqlMapper.addMessage(messages);
        return Result.getInstance();
    }

    @Override
    public Result delMessage(String msgId) {
        mysqlMapper.delMessage(msgId);
        return Result.getInstance();
    }

    @Override
    public Result queryMessage(String flag) {
        return queryMessageById(null, flag);
    }

    @Override
    public Result queryMessageById(String msgId, String flag) {
        Result result = Result.getInstance();
        String requetIp = CommUtils.getClientIpByMDC();
        List<Map<String, Object>> messageList;
        if("1".equals(flag)) {
            messageList = mysqlMapper.queryMessage(msgId, requetIp);
        } else {
            messageList = mysqlMapper.queryMessage(msgId, "");
        }
        List<Map<String, Object>> msgList = new ArrayList<>();
        for (Map<String, Object> map : messageList) {
            Map<String, Object> msgMap = new HashMap<>();
            String clientIp = (String) map.get("ip");
            String userName = userService.getUserName(clientIp);
            if (StringUtil.isEmpty(userName)) {
                userName = "游客";
            }
            msgMap.put("id", map.get("id"));
            msgMap.put("ip", clientIp);
            msgMap.put("name", userName+"("+clientIp+")");
            msgMap.put("content", map.get("info"));
            msgMap.put("time", (map.get("time")+""));
            msgList.add(msgMap);
        }
        int count = mysqlMapper.queryMessageCount();
        Map<String, Object> map = new HashMap<>();
        map.put("msgList", msgList);
        map.put("count", count);
        String requetName = userService.getUserName(requetIp);
        map.put("userName", requetName);
        result.setBody(map);
        return result;
    }

}
