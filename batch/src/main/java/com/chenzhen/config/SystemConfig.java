package com.chenzhen.config;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.Set;

@Slf4j
public class SystemConfig {

    public static void setProperties() {
        try {
            Properties properties = getEnvProperties();
            Set<String> set = properties.stringPropertyNames();
            set.forEach((s) -> {
                if (!"servers".equals(s)) {
                    System.setProperty(s, properties.getProperty(s, ""));
                }
            });
            String[] servers = properties.getProperty("servers").split("\\|");
            run(properties, servers);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    private static void run(Properties properties, String[] servers) {
        for (String server : servers) {
            new Thread(() -> {
                start(properties, server);
            }).start();
        }
    }

    private static void start(Properties properties, String server) {
        while (true) {
            Socket socket = null;
            OutputStreamWriter writer = null;
            BufferedReader reader = null;
            try {
                JSONObject jsonObject = new JSONObject();
                String serverIp = server.split(":", -1)[0];
                String serverPort = server.split(":", -1)[1];
                socket = new Socket(serverIp, Integer.parseInt(serverPort));
                writer = new OutputStreamWriter(socket.getOutputStream());
                Set<String> set = properties.stringPropertyNames();
                set.forEach((s) -> {
                    if (!"servers".equals(s)) {
                        jsonObject.put(s, properties.getProperty(s, ""));
                    }
                });
                writer.write(jsonObject.toJSONString());
                writer.flush();
                Thread.sleep(3000);
            } catch (Exception e) {
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                }
            }

        }
    }

    public static Properties getEnvProperties() {
        Properties properties = new Properties();
        InputStream is = null;
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResources("env.properties")[0];
            properties.load(resource.getInputStream());
        } catch (Exception e) {
            try {
                is = SystemConfig.class.getClassLoader().getResourceAsStream("env.properties");
                properties.load(is);
            } catch (Exception e1) {
                System.exit(1);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e1) {
                    log.info(e1.getMessage());
                }
            }
        }
            return properties;
    }
}
