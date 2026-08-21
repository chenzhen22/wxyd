package com.chenzhen.config;

import cfca.yuzhi.fep.toolkit.ClientContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class CFCAConfig {

    @Value("${cfca.socketServerIP}")
    private String socketServerIP;
    @Value("${cfca.socketServerPort}")
    private int socketServerPort;
    @Value("${cfca.connectTimeout}")
    private int connectTimeout;
    @Value("${cfca.readTimeout}")
    private int readTimeout;

    @PostConstruct
    public void init(){
        ClientContext.initSocket(socketServerIP, socketServerPort, connectTimeout, readTimeout);
    }
}
