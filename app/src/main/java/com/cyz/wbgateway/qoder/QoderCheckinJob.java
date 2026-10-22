package com.cyz.wbgateway.qoder;

import com.cyz.mapper.gateway.WbGatewayMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Qoder 每日自动签到（领取 Credits）。
 * <p>
 * 上游每天 10:00（UTC+8）刷新新一轮，重复领取幂等（replayed=true 不再发奖），
 * 所以 10:10 / 20:10 各尝试一次，白天失败（未开机/网络抖动）晚上还能补。
 */
@Component
@Slf4j
public class QoderCheckinJob {

    @Autowired
    private QoderClient client;

    @Autowired
    private WbGatewayMapper wbGatewayMapper;

    @Scheduled(cron = "0 10 10,20 * * ?")
    public void daily() {
        try {
            Integer enabled = wbGatewayMapper.queryQoderAutoCheckin();
            if (enabled != null && enabled == 0) return;
            if (client.credential() == null) return;
            Map<String, Object> r = client.claim();
            log.info("[QODER] 每日自动签到：{}", r.get("message"));
        } catch (Exception e) {
            log.warn("[QODER] 每日自动签到异常：{}", e.getMessage());
        }
    }
}
