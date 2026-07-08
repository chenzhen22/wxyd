package com.chenzhen.adapter;

import com.chenzhen.mapper.BatchMapper;
import com.chenzhen.pojo.Message;
import com.chenzhen.util.HttpUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Component
public class MMLoginAdapter implements SocketMessageAdapter {

    @Resource
    BatchMapper batchMapper;

    @Override
    public void handleMessage(Message message) {
        try {
            String results = (String) HttpUtil.sendAndReceiveHttps(message.getMessage(), message.getUrl());
            if(StringUtils.hasText(results)) {
                Message msg = new Message(message.getType(),message.getUrl(),message.getMessage(),results,"1",message.getDate(),"","");
                batchMapper.updateSocketMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
