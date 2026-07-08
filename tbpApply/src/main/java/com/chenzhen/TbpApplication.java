package com.chenzhen;

import com.chenzhen.config.SystemConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAspectJAutoProxy
public class TbpApplication {

    public static void main(String[] args) {
        SystemConfig.getProperties();
        SpringApplication.run(TbpApplication.class, args);
        System.out.println("tbpApply启动成功");
    }
}
