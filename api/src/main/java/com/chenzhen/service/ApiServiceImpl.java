package com.chenzhen.service;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.fegin.TbpApplyFeign;
import com.chenzhen.pojo.Doc;
import com.chenzhen.pojo.DocumentFile;
import com.chenzhen.pojo.Result;
import com.chenzhen.util.BtoAAtoB;
import com.chenzhen.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ApiServiceImpl implements ApiService {

    @Resource
    TbpApplyFeign tbpApplyFeign;

    @Override
    public Object tbphx(String transData, HttpServletRequest request) {
        log.info("request data：{}, ip: {} ", transData, CommUtils.getClientIp(request));
        Result result = Result.getInstance();
        try {
            transData = BtoAAtoB.atob(transData);
            transData = URLDecoder.decode(transData, "UTF-8");
            if(!transData.contains("||")) {
                long timestamp = System.currentTimeMillis();
                transData = transData+"||"+timestamp;
                transData = URLEncoder.encode(transData, "UTF-8");
                transData = BtoAAtoB.btoa(transData);
                Map<String, String> map = new HashMap();
                map.put("transData", transData);
                result.setBody(map);
                return result;
            }
            String[] ss = transData.split("\\|\\|", -1);
            long delTime = Long.parseLong(ss[1]) - System.currentTimeMillis();
            if (Math.abs(delTime) > 3000) {
                return result;
            }
            String reqString = ss[0];
            reqString = reqString.replace("\\", "\\\\");
            JSONObject json = JSONObject.parseObject(reqString);
            log.info("request data：{}", json);
            String action = (String) json.get("action");
            json.remove("action");
            Method[] methods = TbpApplyFeign.class.getMethods();
            Result res = Result.getInstance();
            res.setBody(json);
            for (Method method : methods) {
                if (action.equals(method.getName())) {
                    return method.invoke(tbpApplyFeign, res);
                }
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String docQryAll(String type) {
        try {
            Result result = Result.getInstance();
            result.setBody(type);
            result = tbpApplyFeign.docQryAll(result);
            return (String) result.getBody();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String docQry(String docname) {
        Result result = Result.getInstance();
        result.setBody(docname);
        result = tbpApplyFeign.docQry(result);
        return (String) result.getBody();
    }

    @Override
    public void uploadDoc(Doc doc) {
        Result result = Result.getInstance();
        result.setBody(doc);
        tbpApplyFeign.uploadDoc(result);
    }

    @Override
    public void uploadDocumentFile(DocumentFile docFile) {
        Result result = Result.getInstance();
        result.setBody(docFile);
        tbpApplyFeign.uploadDocumentFile(result);
    }

    @Override
    public Object queryDocumentFileList(String fileName, String clientIp) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileName", fileName);
        jsonObject.put("clientIp", clientIp);
        Result result = Result.getInstance();
        result.setBody(jsonObject);
        return tbpApplyFeign.queryDocumentFileList(result);
    }

    @Override
    public int deleteDocumentFile(String fileUUID) {
        Result result = Result.getInstance();
        result.setBody(fileUUID);
        result = tbpApplyFeign.deleteDocumentFile(result);
        return (int) result.getBody();
    }

}
