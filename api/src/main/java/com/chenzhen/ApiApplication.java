package com.chenzhen;

import com.chenzhen.config.SystemConfig;
import com.chenzhen.util.SocketUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import java.io.FileInputStream;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ApiApplication {
    public static void main(String[] args) {
        SystemConfig.getProperties();
        SpringApplication.run(ApiApplication.class, args);
        System.out.println("api启动成功");
    }
}
