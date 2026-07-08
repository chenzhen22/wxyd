package com.chenzhen.service;

import com.chenzhen.adapter.SocketMessageAdapter;
import com.chenzhen.factory.SocketMessageFactory;
import com.chenzhen.mapper.BatchMapper;
import com.chenzhen.pojo.Message;
import com.chenzhen.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class ScheduledService {


    @Resource
    BatchMapper batchMapper;

    @Autowired
    SocketMessageFactory socketMessageFactory;

    @Scheduled(fixedRate = 200)
    private void refreshMessage() {
        List<Message> list = batchMapper.queryMessageList();

        list.forEach(message -> {
            log.info("待处理通道消息：{}", list);
            String serverIp = message.getServerIp();
            if (!"1".equals(message.getStatus())) {
                SocketMessageAdapter messageAdapter = socketMessageFactory.getInstall(message.getType());
                messageAdapter.handleMessage(message);
            }
        });

    }

    @Scheduled(cron = "0 0/30 * * * ?")
    private void shutdown() {
        LocalDateTime localDateTime = LocalDateTime.now();
        int hour = localDateTime.getHour();
        if(hour >= 21) {
            try {
                log.info("shutdown time：{}", CommUtils.getDateString("yyyy/MM/dd HH:mm:ss"));
                Runtime.getRuntime().exec("shutdown -h");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Scheduled(cron = "0 0 10 * * ?")
    private void delete() {
        batchMapper.deleteSocketMessage();
        batchMapper.deleteRequestlog();
    }
}
