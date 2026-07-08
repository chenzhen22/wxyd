package com.chenzhen.controller;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.pojo.DocumentFile;
import com.chenzhen.pojo.Result;
import com.chenzhen.service.ApiService;
import com.chenzhen.util.CommUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/doc")
public class DocController implements CommController{

    private String path = "./docfile/";

    @Autowired
    ApiService apiService;

    @ResponseBody
    @RequestMapping("/upload")
    public void upload(HttpServletRequest request, HttpServletResponse response, @RequestParam("file") MultipartFile file) throws IOException {
        String clientIp = CommUtils.getClientIp(request);
        String fileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        File filepath = new File(path + uuid + "/" + clientIp);
        if (!filepath.exists()) {
            filepath.mkdirs();
        }
        File fileNew = new File(filepath.getAbsolutePath(), fileName);
        if (fileNew.exists()) {
            fileNew.delete();
        }
        InputStream input = null;
        DataOutputStream pw = null;
        try {
            fileNew.createNewFile();

            input = file.getInputStream();
            byte[] bytes = new byte[1024];
            int len = -1;
            pw = new DataOutputStream(new FileOutputStream(fileNew));
            while ((len = input.read(bytes)) != -1) {
                pw.write(bytes, 0, len);
            }
            pw.flush();
            String creatTime = CommUtils.getDateString("yyyyMMddHHmmss");
            DocumentFile docFile = new DocumentFile(uuid, clientIp, null, fileName, creatTime);
            apiService.uploadDocumentFile(docFile);

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-type", "application/json");
            Result result = Result.getInstance();
            JSONObject json = (JSONObject) JSONObject.toJSON(result);
            response.getOutputStream().write((json.toString()).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @ResponseBody
    @RequestMapping("/list")
    public Object list(@RequestParam("fileName") String fileName, @RequestParam("type") String type) {
        return apiService.queryDocumentFileList(fileName, type);
    }

    @ResponseBody
    @RequestMapping("/download")
    public void download(@RequestParam("fileUUID") String fileUUID, HttpServletResponse response) {
        File filedir = new File(path + fileUUID);
        File filedirIp = filedir.listFiles()[0];
        File file = filedirIp.listFiles()[0];
        String fileName = file.getName();
        ServletOutputStream outputStream = null;
        try {
            String encodeFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
            InputStream in = new FileInputStream(file);
            outputStream = response.getOutputStream();
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodeFileName + "\";filename*=UTF-8''" + encodeFileName);
            response.setContentType("application/octet-stream");
            if (fileName.endsWith("xls") || fileName.endsWith("xlsx")) {
                response.setContentType("application/vnd.ms-excel");
            }
            if (fileName.endsWith("pdf")) {
                response.setContentType("application/pdf");
            }
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        } catch (Exception ex) {

        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    @ResponseBody
    @RequestMapping("/delete")
    public Object delete(@RequestParam("fileUUID") String fileUUID) {
        int res = apiService.deleteDocumentFile(fileUUID);
        Result result = Result.getInstance();
        if (res == 1) {
            File filepath = new File(path + fileUUID);
            FileUtil.del(filepath);
        } else if (res == 2) {
            result.setErrorCode("000003");
            result.setErrorMsg("只能删除自己上传的文件");
            return result;
        }
        return result;
    }
}
