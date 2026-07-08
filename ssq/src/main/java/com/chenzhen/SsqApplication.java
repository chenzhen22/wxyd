package com.chenzhen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SsqApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsqApplication.class, args);
    }
}
