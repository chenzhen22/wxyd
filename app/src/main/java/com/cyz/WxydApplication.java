package com.cyz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sole Spring Boot entry point for the merged app.
 * <p>
 * {@code env.properties} (classpath) carries environment-specific secrets and
 * overrides for {@code ${datasource.*}} placeholders that application.yml
 * references. It replaces the former Nacos-backed value injection; loaded here
 * as a property source so {@code @Value} and YAML placeholder resolution both
 * see it.
 *
 * <p>{@code @EnableScheduling}：羊毛雷达（RadarPushJob）与巡检日报（PatrolJob）
 * 的定时推送依赖此注解，项目首个调度开关。
 */
@SpringBootApplication
@EnableScheduling
@PropertySource(value = "classpath:env.properties", ignoreResourceNotFound = true)
public class WxydApplication {
    public static void main(String[] args) {
        SpringApplication.run(WxydApplication.class, args);
        System.out.println("wxyd启动成功");
    }
}
