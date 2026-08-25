package com.cyz.service;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.Messages;
import com.cyz.pojo.Result;
import com.cyz.pojo.User;
import com.cyz.util.CommUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    MysqlMapper mysqlMapper;

    @Override
    public Result addMessage(Long userId, String message) {
        String clientIp = CommUtils.getClientIpByMDC();
        Messages messages = new Messages(userId, clientIp, message);
        mysqlMapper.addMessage(messages);
        mysqlMapper.trimMessages(); // 最多保留 50 条，超出删最旧
        return Result.getInstance();
    }

    @Override
    public Result delMessage(String msgId) {
        mysqlMapper.delMessage(msgId);
        return Result.getInstance();
    }

    @Override
    public int delMessageByUser(Long userId, String msgId) {
        return mysqlMapper.delMessageByUser(userId, msgId);
    }

    @Override
    public Result queryMessage(Long userId, String flag) {
        Result result = Result.getInstance();
        Long filterUserId = "1".equals(flag) ? userId : null;
        List<Map<String, Object>> messageList = mysqlMapper.queryMessage(null, filterUserId);
        int count = mysqlMapper.queryMessageCount();
        String displayName = null;
        String username = null;
        if (userId != null) {
            User u = mysqlMapper.queryUserById(userId);
            if (u != null) {
                displayName = u.getDisplayName();
                username = u.getUsername();
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("msgList", messageList);
        map.put("count", count);
        map.put("displayName", displayName);
        map.put("username", username);
        result.setBody(map);
        return result;
    }

    @Override
    public Result queryMessageById(String msgId, String flag) {
        Result result = Result.getInstance();
        List<Map<String, Object>> messageList = mysqlMapper.queryMessage(msgId, null);
        Map<String, Object> map = new HashMap<>();
        map.put("msgList", messageList);
        map.put("count", mysqlMapper.queryMessageCount());
        map.put("userName", "");
        result.setBody(map);
        return result;
    }

}
