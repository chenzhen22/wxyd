package com.chenzhen.config;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.Set;

@Slf4j
public class SystemConfig {

    public static void setProperties() {

        FileInputStream is = null;
        try {
            Properties properties = new Properties();
            is = new FileInputStream(new File("/apps/data/wxyd/env.properties"));
            properties.load(is);
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
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                log.info(e.getMessage());
            }
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
                Thread.sleep(5000);
            } catch (Exception e) {
                log.info(e.getMessage());
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
                    log.info(e.getMessage());
                }
            }

        }
    }
}
