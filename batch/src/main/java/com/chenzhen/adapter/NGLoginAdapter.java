package com.chenzhen.adapter;

import com.chenzhen.mapper.BatchMapper;
import com.chenzhen.pojo.Message;
import com.chenzhen.util.HttpUtil;
import com.chenzhen.util.SocketUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Component
public class NGLoginAdapter implements SocketMessageAdapter {

    @Resource
    BatchMapper batchMapper;

    @Override
    public void handleMessage(Message message) {
        try {
            String url = message.getUrl();
            String[] urls = url.split(":", -1);
            SocketUtil socket = new SocketUtil(urls[0], Integer.parseInt(urls[1]), "GBK");
            String results = (String) socket.sendAndReceiveHttps(message.getMessage());
            if(StringUtils.hasText(results)) {
                Message msg = new Message(message.getType(),message.getUrl(),message.getMessage(),results,"1",message.getDate(),"","","");
                batchMapper.updateSocketMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
