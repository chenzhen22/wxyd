package com.chenzhen.config;

import com.chenzhen.annotation.EnableIfpRedissonHttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(prefix = "ifp.session", name = "persistent-type", havingValue = "REDIS")
@EnableIfpRedissonHttpSession(maxInactiveIntervalInSeconds = 600)
public class RedissonSessionConfiguration {
}
