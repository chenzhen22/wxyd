package com.chenzhen.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ConfigurationProperties(prefix = "msg.sftp")
@Getter
@Setter
public class MsgConfig {

    private String ip;
    private String user;
    private String pwd;
    private String path;

    private String ip_y;
    private String user_y;
    private String pwd_y;
    private String path_y;
}
