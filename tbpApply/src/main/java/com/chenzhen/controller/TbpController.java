package com.chenzhen.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.config.ProperConfig;
import com.chenzhen.mapper.mysqlMapper.MysqlMapper;
import com.chenzhen.pojo.*;
import com.chenzhen.service.TbpService;
import com.chenzhen.util.CommUtils;
import com.chenzhen.util.SSHUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.microsoft.schemas.office.x2006.encryption.CTKeyEncryptor.Uri.type;

@RestController
@Slf4j
public class TbpController implements CommController{

    @Autowired
    TbpService tbpService;

    @Resource
    MysqlMapper mysqlMapper;

    @ResponseBody
    @RequestMapping("start")
    public Result start(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String searchFiled = (String) map.get("searchFiled");
        String type = (String) map.get("type");
        String host = (String) map.get("host");
        String project = (String) map.get("project");
        result = Result.getInstance();
        if (!StringUtils.hasText(host)) {
            return result;
        }
        if (!StringUtils.hasText(project)) {
            return result;
        }
        String date = CommUtils.getDateString("yyyyMMddHHmmssS");
        String comd = "grep " + searchFiled + " /logs/app_logs/" + project + "/" + project + ".log";
        if ("1".equals(type)) {
            comd = "zgrep " + searchFiled + " /logs/app_logs/history/" + project + "/*/**";
        }
        SocketMessage socketMessage = new SocketMessage("2", host, comd, "", "0", date, CommUtils.getClientIpByMDC(), CommUtils.getHostAddress());
        socketMessage.setHandleIp(CommUtils.getParamValue("handleIp"));
        mysqlMapper.addSocketMessage(socketMessage);
        int index = 0;
        while (true) {
            String revice = mysqlMapper.querySocketRevice(socketMessage.getDate());
            if (StringUtils.hasText(revice)) {
                Session sftpSession = SSHUtil.createSession(new Sshbean(CommUtils.getHostAddress(), 22, null, null));
                ChannelSftp sftpChannel = (ChannelSftp) sftpSession.openChannel("sftp");
                sftpChannel.connect();
                int ind = revice.lastIndexOf("/");
                String path = revice.substring(0, ind + 1);
                String fileName = revice.substring(ind + 1);
                InputStream in = SSHUtil.getFile(fileName, sftpChannel, path);

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
                String msg = null;
                String msgResult = "";
                while ((msg = reader.readLine()) != null) {
                    msg = msg.replaceAll("<", "&lt;");
                    msg = msg.replaceAll(">", "&gt;");
                    msgResult += msg + "<br>";
                }
                JSONObject result2 = new JSONObject();
                result2.put("result", msgResult);

                reader.close();
                sftpSession.disconnect();
                result.setBody(result2.toJSONString());
                return result;
            }

            if (index > 60) {
                mysqlMapper.updateSocketMsgStatus(date, "1");
                return null;
            }
            index++;
            if ("1".equals(type)) {
                Thread.sleep(5000);
            } else {
                Thread.sleep(500);
            }

        }
    }

    @GetMapping("/dec")
    public Result dec(@Param("type") String type, @Param("password") String password) {
        return tbpService.dec(type, password);
    }

    @RequestMapping("/getParamValue")
    public Result getParamValue(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String key = (String) map.get("key");
        String value = CommUtils.getParamValue(key);
        Result res = Result.getInstance();
        res.setBody(value);
        return res;
    }

    @RequestMapping("/docQry")
    public Result docQry(@RequestBody Result result) throws IOException {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String docname = (String) map.get("docname");
        return tbpService.docQry(docname);
    }

    @RequestMapping("/createDoc")
    public void createDoc(@Param("type") String type) throws IOException {
        tbpService.createDoc(type);
    }

    @ResponseBody
    @RequestMapping("uploadDoc")
    public Result uploadDoc(@RequestBody Result result) {
        Doc doc = BeanUtil.fillBeanWithMap((Map<?, ?>) result.getBody(), new Doc(), false);
        tbpService.uploadDoc(doc);
        return Result.getInstance();
    }

    @RequestMapping("queryDocAll")
    public Result queryDocAll(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String type = (String) map.get("type");
        return tbpService.queryDocAll(type);
    }

    @RequestMapping("addDocFile")
    public Result addDocFile(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String type = (String) map.get("type");
        return tbpService.queryDocAll(type);
    }

    @RequestMapping("/docQryAll")
    public Result docQryAll(@RequestBody Result result) throws IOException {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String type = (String) map.get("type");
        String filename = tbpService.docQryAll(type);
        result = Result.getInstance();
        result.setBody(filename);
        return result;
    }

    @ResponseBody
    @RequestMapping("uploadDocumentFile")
    public Result uploadDocumentFile(@RequestBody Result result) {
        DocumentFile documentFile = BeanUtil.fillBeanWithMap((Map<?, ?>) result.getBody(), new DocumentFile(), false);
        tbpService.uploadDocumentFile(documentFile);
        return Result.getInstance();
    }

    @ResponseBody
    @RequestMapping("queryDocumentFileList")
    public Result queryDocumentFileList(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String fileName = (String) map.get("fileName");
        String type = (String) map.get("type");
        String clientIp = "";
        if("1".equals(type)) {
            clientIp = CommUtils.getClientIpByMDC();
        }
        return tbpService.queryDocumentFileList(fileName, clientIp);
    }

    @ResponseBody
    @RequestMapping("deleteDocumentFile")
    public Result deleteDocumentFile(@RequestBody Result result) {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String fileUUID = (String) map.get("fileUUID");
        String client = CommUtils.getClientIpByMDC();
        String adminIp = CommUtils.getParamValue("mailIp");
        result = Result.getInstance();
        if(client.equals(adminIp)) {
            result.setBody(tbpService.deleteDocumentFile(fileUUID));
            return result;
        }
        DocumentFile documentFile = tbpService.queryDocumentFile(fileUUID);
        if(ObjectUtil.isNotEmpty(documentFile)) {
            if(!client.equals(documentFile.getClientIp())) {
                result.setBody(2);
                return result;
            }
        }
        result.setBody(tbpService.deleteDocumentFile(fileUUID));
        return result;
    }
}