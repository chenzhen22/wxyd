package com.chenzhen.util;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Data
public class SocketUtil {
    public static int msgHeadLen = 8;


    private String ip;
    private int port;
    private String encoed;

    public SocketUtil(String ip, int port, String encoed) {
        this.ip = ip;
        this.port = port;
        this.encoed = encoed;
    }

    public static byte[] readPackage(InputStream in) throws Exception {
        int contentLength = msgHeadLen;
        byte[] revMessage = new byte[contentLength];
        int off = 0;
        while (off < contentLength) {
            int len = in.read(revMessage, off, contentLength - off);
            if (len <= 0) {
                throw new Exception("connect is close, while read head length: " + new String(revMessage));
            }
            off += len;
        }
        contentLength = Integer.parseInt(new String(revMessage).trim());
        byte[] contentBuf = new byte[contentLength];
        off = 0;
        while (off < contentLength) {
            int len = in.read(contentBuf, off, contentLength - off);
            if (len <= 0) {
                break;
            }
            off += len;
        }
        return contentBuf;
    }

    public Object sendAndReceiveHttps(String message) throws Exception {
        OutputStream out = null;
        InputStream in = null;


        InetSocketAddress socketAddr = new InetSocketAddress(ip, port);
        Socket socket = new Socket();
        try {
            socket.setSoLinger(true, 0);
            socket.setSoTimeout(10000);
            socket.setTcpNoDelay(true);
            socket.connect(socketAddr);
            out = socket.getOutputStream();
            in = socket.getInputStream();
            log.info("{}:{}连接成功", ip, port);
        } catch (ConnectException ce) {
            ce.printStackTrace();
        } catch (Exception e) {
            log.info("{}:{}连接失败", ip, port);
            e.printStackTrace();
        }

        String resultMsg = null;
        try {
            int msgLen = CommUtils.getStringLen(message, encoed);
            int len = String.valueOf(msgLen).length();
            StringBuffer msg = new StringBuffer();
            if (msgHeadLen > 0) {
                for (int i = 0; i < msgHeadLen - len; ++i) {
                    msg.append(0);
                }
                msg.append(msgLen);
            }
            msg.append(message);
            log.info("send message:{}", message);

            out.write(msg.toString().getBytes(encoed));
            byte[] msgBytes = readPackage(in);
            resultMsg = new String(msgBytes, encoed);
            log.info("receive message:{}", resultMsg);
            return resultMsg;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != socket) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return resultMsg;
    }
}
