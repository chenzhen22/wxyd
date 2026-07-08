package com.chenzhen.adapter;

import com.chenzhen.mapper.BatchMapper;
import com.chenzhen.pojo.Message;
import com.chenzhen.pojo.Sshbean;
import com.chenzhen.util.CommUtils;
import com.chenzhen.util.SSHUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.InputStream;

@Component
@Slf4j
public class StartAdapter implements SocketMessageAdapter{

    @Resource
    BatchMapper batchMapper;

    @Override
    public void handleMessage(Message message) {
        String serverIp = message.getServerIp();
        Session sshSession = null;
        ChannelExec execChannel = null;
        InputStream in = null;
        try {
            Sshbean sshbean = new Sshbean(message.getUrl(), 22, null, null);
            sshSession = SSHUtil.createSession(sshbean);
            log.info("handleMessage is connect success,", sshSession);
            execChannel = (ChannelExec) sshSession.openChannel("exec");
            in = SSHUtil.exec(message.getMessage(), execChannel);
            String fileName = CommUtils.getDateString("yyyyMMddHHmmssS")+".txt";
            String date = CommUtils.getDateString("yyyyMMdd");
            String filePath = "/logs/testSpbt/"+date;
            Session sftpSession = SSHUtil.createSession(new Sshbean(serverIp, 22, "apps", "apps"));
            ChannelSftp sftpChannel = (ChannelSftp) sftpSession.openChannel("sftp");
            sftpChannel.connect(60000);
            SSHUtil.upload(in, fileName, sftpChannel, filePath);
            sftpSession.disconnect();
            Message msg = new Message(message.getType(),message.getUrl(),message.getMessage(),filePath+"/"+fileName,"1",message.getDate(), "", "","");
            batchMapper.updateSocketMessage(msg);
        } catch (Exception e) {
            log.info("handleMessage fail reason:", e);
            e.printStackTrace();
        } finally {
            sshSession.disconnect();
            execChannel.disconnect();
        }
    }
}
