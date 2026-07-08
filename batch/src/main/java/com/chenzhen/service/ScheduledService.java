package com.chenzhen.service;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.adapter.SocketMessageAdapter;
import com.chenzhen.config.MsgConfig;
import com.chenzhen.factory.SocketMessageFactory;
import com.chenzhen.feign.TbpApplyFeign;
import com.chenzhen.mapper.BatchMapper;
import com.chenzhen.pojo.Message;
import com.chenzhen.pojo.Result;
import com.chenzhen.pojo.Sshbean;
import com.chenzhen.util.CommUtils;
import com.chenzhen.util.SSHUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScheduledService {

    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    private static final Object lock3 = new Object();
    private static final Object lock4 = new Object();
    @Resource
    BatchMapper batchMapper;
    @Autowired
    SocketMessageFactory socketMessageFactory;
    @Autowired
    TbpApplyFeign tbpApplyFeign;
    @Autowired
    MsgConfig msgConfig;

    @Scheduled(fixedRate = 200)
    private void refreshMessage() {
        synchronized (lock1) {
            List<Message> list = batchMapper.queryMessageList();
            if (list.size() > 0) {
                System.out.println(list.get(0));
            }

            list.forEach(message -> {
                log.info("待处理通道消息：{}", list);
                String handleIp = message.getHandleIp();
                if (!"1".equals(message.getStatus()) && handleIp.equals(CommUtils.getParamValue("handleIp"))) {
                    SocketMessageAdapter messageAdapter = socketMessageFactory.getInstall(message.getType());
                    messageAdapter.handleMessage(message);
                }
            });
        }
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    protected void shutdown() {
        LocalDateTime localDateTime = LocalDateTime.now();
        int hour = localDateTime.getHour();
        if (hour >= 21) {
            try {
                log.info("shutdown time：{}", CommUtils.getDateString("yyyy/MM/dd HH:mm:ss"));
                Runtime.getRuntime().exec("shutdown -h");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Scheduled(fixedRate = 200)
    private void handleApiMsgLocal() {
        synchronized (lock2) {
            File fileDir = new File("./msg/");
            if (fileDir.exists()) {
                File[] files = fileDir.listFiles();
                List<File> sendFilesCom = Arrays.stream(files).filter(file -> file.getName().endsWith(".send")).collect(Collectors.toList());
                List<File> revFiles = Arrays.stream(files).filter(file -> file.getName().endsWith(".rev")).collect(Collectors.toList());
                List<File> sendFiles = new ArrayList<>();
                sendFilesCom.forEach(sendFile -> {
                    if (revFiles.isEmpty()) {
                        sendFiles.add(sendFile);
                    } else {
                        String fileName = sendFile.getName().substring(0, sendFile.getName().lastIndexOf("."));
                        boolean addFlag = true;
                        for (File f : revFiles) {
                            if (f.getName().contains(fileName)) {
                                addFlag = false;
                            }
                        }
                        if (addFlag) {
                            sendFiles.add(sendFile);
                        }

                    }

                });
                sendFiles.forEach(file -> {
                    BufferedReader br = null;
                    PrintWriter pw = null;
                    try {
                        String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                        String reqString = br.readLine();
                        if (StringUtils.hasText(reqString)) {
                            JSONObject json = JSONObject.parseObject(reqString);
                            log.info("request data：{}", json);
                            String action = (String) json.get("action");
                            json.remove("action");
                            Method[] methods = TbpApplyFeign.class.getMethods();
                            Result res = Result.getInstance();
                            String traceId = (String) json.get("traceId");
                            json.remove("traceId");
                            res.setTraceId(traceId);
                            String clientIp = (String) json.get("clientIp");
                            json.remove("clientIp");
                            res.setClientIp(clientIp);
                            res.setBody(json);
                            for (Method method : methods) {
                                if (action.equals(method.getName())) {
                                    Result result = (Result) method.invoke(tbpApplyFeign, res);
                                    File fileRev = new File("./msg/" + fileName + ".rev");
                                    if (!fileRev.exists()) {
                                        fileRev.createNewFile();
                                        try {
                                            if (!System.getProperty("os.name").toLowerCase().startsWith("win")) {
                                                Runtime.getRuntime().exec("chmod 777 " + fileRev.getAbsolutePath());
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileRev), StandardCharsets.UTF_8));
                                    pw.write(JSONObject.toJSONString(result));
                                    pw.flush();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (pw != null) {
                            pw.close();
                        }
                    }

                });
            }

        }
    }

    @Scheduled(fixedRate = 200)
    private void handleApiMsgSftp() {
        synchronized (lock3) {
            Sshbean sshbean = new Sshbean(msgConfig.getIp(), 22, msgConfig.getUser(), msgConfig.getPwd());
            handleApiMsgSftp(sshbean, msgConfig.getPath());
        }
    }

    @Scheduled(fixedRate = 200)
    private void handleApiMsgSftp_Y() {
        synchronized (lock4) {
            Sshbean sshbean = new Sshbean(msgConfig.getIp_y(), 22, msgConfig.getUser_y(), msgConfig.getPwd_y());
            handleApiMsgSftp(sshbean, msgConfig.getPath_y());
        }
    }

    private void handleApiMsgSftp(Sshbean sshbean, String path) {
        Session sftpSession = null;
        ChannelSftp sftpChannel = null;
        try {
            sftpSession = SSHUtil.createSession(sshbean);
            sftpChannel = (ChannelSftp) sftpSession.openChannel("sftp");
            sftpChannel.connect();
            Vector<ChannelSftp.LsEntry> vector = sftpChannel.ls(path);
            List<String> sendFilesCom = new ArrayList<>();
            List<String> revFiles = new ArrayList<>();
            List<String> sendFiles = new ArrayList<>();
            vector.forEach((v) -> {
                String filename = v.getFilename();
                if (filename.endsWith("send")) {
                    sendFilesCom.add(filename);
                }
                if (filename.endsWith("rev")) {
                    revFiles.add(filename);
                }
            });
            sendFilesCom.forEach(sendFile -> {
                if (revFiles.isEmpty()) {
                    sendFiles.add(sendFile);
                } else {
                    String fileName = sendFile.substring(0, sendFile.lastIndexOf("."));
                    boolean addFlag = true;
                    for (String f : revFiles) {
                        if (f.contains(fileName)) {
                            addFlag = false;
                        }
                    }
                    if (addFlag) {
                        sendFiles.add(sendFile);
                    }

                }
            });
            for (String file : sendFiles) {
                BufferedReader br = null;
                PrintWriter pw = null;
                try {
                    String fileName = file.substring(0, file.lastIndexOf("."));
                    br = new BufferedReader(new InputStreamReader(sftpChannel.get(path + file), StandardCharsets.UTF_8));
                    String reqString = br.readLine();
                    if (StringUtils.hasText(reqString)) {
                        JSONObject json = JSONObject.parseObject(reqString);
                        log.info("request data：{}", json);
                        String action = (String) json.get("action");
                        json.remove("action");
                        Method[] methods = TbpApplyFeign.class.getMethods();
                        Result res = Result.getInstance();
                        String traceId = (String) json.get("traceId");
                        json.remove("traceId");
                        res.setTraceId(traceId);
                        String clientIp = (String) json.get("clientIp");
                        json.remove("clientIp");
                        res.setClientIp(clientIp);
                        res.setBody(json);
                        for (Method method : methods) {
                            if (action.equals(method.getName())) {
                                Result result = (Result) method.invoke(tbpApplyFeign, res);
                                File fileDir = new File("/apps/data/wxyd/msg/");
                                if (!fileDir.exists()) {
                                    fileDir.mkdirs();
                                }
                                File fileRev = new File("/apps/data/wxyd/msg/" + fileName + ".rev");
                                if (!fileRev.exists()) {
                                    fileRev.createNewFile();
                                }
                                pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileRev), StandardCharsets.UTF_8));
                                pw.write(JSONObject.toJSONString(result));
                                pw.flush();
                                sftpChannel.put(fileRev.getAbsolutePath(), path);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (pw != null) {
                        pw.close();
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            SSHUtil.close(sftpSession, sftpChannel);
        }
    }

    @Scheduled(cron = "0 0 10 * * ?")
    private void delete() {
        batchMapper.deleteSocketMessage();
        batchMapper.deleteRequestlog();
    }
}
