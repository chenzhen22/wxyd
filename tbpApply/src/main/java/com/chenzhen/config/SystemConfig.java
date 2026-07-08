package com.chenzhen.config;

import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SystemConfig {

    public static void getProperties() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        BufferedReader reader = null;
        try {
            serverSocket = new ServerSocket(26210);
            socket = serverSocket.accept();
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String clientMsg = reader.readLine();
            JSONObject jsonObject = JSONObject.parseObject(clientMsg);
            jsonObject.entrySet().forEach((map) -> {
                System.setProperty(map.getKey(), String.valueOf(map.getValue()));
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (socket != null) {
                    socket.close();
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
