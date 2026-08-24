package com.chenzhen.controller;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.config.ProperConfig;
import com.chenzhen.constant.ErrorEnum;
import com.chenzhen.pojo.Doc;
import com.chenzhen.pojo.Result;
import com.chenzhen.service.ApiService;
import com.chenzhen.service.MsgService;
import com.chenzhen.util.CommUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@RestController
public class ApiController implements CommController{

    @Autowired
    ApiService apiService;

    @Autowired
    MsgService msgService;

    @ResponseBody
    @RequestMapping("tbphx.do")
    public Object tbphx(@RequestParam(value = "transData", required = false) String transData, HttpServletRequest request) throws Exception {
        return apiService.tbphx(transData, request);
    }

    @ResponseBody
    @RequestMapping("docQryAll")
    public void docQryAll(@RequestParam("type") String type, HttpServletResponse response) throws Exception {
        String filename = apiService.docQryAll(type);
        String path = "./doc/" + filename;
        downExcel(path, response);

    }

    @ResponseBody
    @RequestMapping("docQry")
    public void docQry(@RequestParam("docname") String docname, HttpServletResponse response) throws Exception {
        String path = apiService.docQry(docname);
        downExcel(path, response);
    }

    @ResponseBody
    @RequestMapping("uploadDoc")
    public void uploadDoc(HttpServletRequest request, HttpServletResponse response, @RequestParam("fileName") MultipartFile file) throws IOException {
        String client = CommUtils.getClientIp(request);
        String uploadDocIP = msgService.getParamValue("uploadDocIP");
        String[] uploadDocIPs = uploadDocIP.split(",", -1);
        boolean flag = false;
        for(String ip : uploadDocIPs) {
            if(client.equals(ip)) {
                flag = true;
                break;
            }
        }
        if(!flag) {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-type", "application/json");
            Result result = Result.getInstance();
            result.setErrorEnum(ErrorEnum.ERROR888888);
            JSONObject json = (JSONObject) JSONObject.toJSON(result);
            response.getOutputStream().write((json.toString()).getBytes());
            return;
        }
        String project = request.getParameter("project");
        String transCode = request.getParameter("transCode");
        String transName = request.getParameter("transName");
        String blName = request.getParameter("blName");
        String type = request.getParameter("type");
        String fileName = file.getOriginalFilename();
        int blindex = blName.indexOf(".");
        String blpath = blName.substring(0, blindex);
        File filepath = new File("./doc/bl/" + project + "/" + blpath);
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
            Doc doc = new Doc(transCode, transName, project, blName, type);
            apiService.uploadDoc(doc);

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

    public static void downExcel(String path, HttpServletResponse response) throws Exception {
        Workbook workBook = CommUtils.getExcelTemplate(path);
        int index = path.lastIndexOf("/");
        String fileName = path.substring(index+1);
        ServletOutputStream outputStream = null;
        try {
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            response.setContentType("application/vnd.ms-excel");
            outputStream = response.getOutputStream();
            workBook.write(outputStream);
            outputStream.flush();
        } catch (IOException ex) {
            throw ex;
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
}
