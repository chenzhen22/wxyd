package com.chenzhen.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.InputStream;
import java.util.Properties;

public class SysConfig {
    public static final String JDBC_URL_KEY = "jdbc.url";
    public static final String JDBC_USERNAME_KEY = "jdbc.username";
    public static final String JDBC_PASSWORD_KEY = "jdbc.password";
    public static final String SYS_CH_NAME = "sys.ch.name";
    public static final String SYS_EN_NAME = "sys.en.name";
    public static final String SYS_MODEL = "sys.model";
    public static final String IN_TAB_NAME = "in.tab.name";
    public static final String OUT_TAB_NAME = "out.tab.name";
    private static SysConfig sysConfig;
    private static Properties properties;

    public static SysConfig getInstance() {
        if (sysConfig == null)
            sysConfig = new SysConfig();

        if (properties == null) {
            properties = new Properties();
            try {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource resource = resolver.getResources("config.txt")[0];
                properties.load(resource.getInputStream());
            } catch (Exception e) {
                try {
                    InputStream is = SysConfig.class.getClassLoader().getResourceAsStream("config.txt");
                    properties.load(is);
                } catch (Exception e1) {
                    System.out.println("找不到文件config.txt！");
                    System.exit(1);
                }
            }
        }
        return sysConfig;
    }

    public static String getPropertyValue(String key) {
        return properties.getProperty(key);
    }
}