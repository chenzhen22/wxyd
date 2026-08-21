package com.chenzhen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WxydApplication {
    public static void main(String[] args) {
        SpringApplication.run(WxydApplication.class, args);
        System.out.println("wxyd启动成功");
    }
}
