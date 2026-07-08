package com.chenzhen;

import com.chenzhen.config.SystemConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients
public class BatchApplication {

    public static void main(String[] args) {
        SystemConfig.setProperties();
        SpringApplication.run(BatchApplication.class, args);
        System.out.println("batch启动成功");
    }
}
