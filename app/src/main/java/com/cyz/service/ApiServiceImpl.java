package com.cyz.service;

import com.alibaba.fastjson.JSONObject;
import com.cyz.pojo.Result;
import com.cyz.util.BtoAAtoB;
import com.cyz.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ApiServiceImpl implements ApiService {

    @Autowired
    private MsgService msgService;

    @Override
    public Object tbphx(String transData, HttpServletRequest request) {
        String clientIp = CommUtils.getClientIp(request);
        log.info("request data：{}, ip: {} ", transData, clientIp);
        Result result = Result.getInstance();
        try {
            transData = BtoAAtoB.atob(transData);
            transData = URLDecoder.decode(transData, "UTF-8");
            if (!transData.contains("||")) {
                long timestamp = System.currentTimeMillis();
                transData = transData + "||" + timestamp;
                transData = URLEncoder.encode(transData, "UTF-8");
                transData = BtoAAtoB.btoa(transData);
                Map<String, String> map = new HashMap<>();
                map.put("transData", transData);
                result.setBody(map);
                return result;
            }
            String[] ss = transData.split("\\|\\|", -1);
            long delTime = Long.parseLong(ss[1]) - System.currentTimeMillis();
            if (Math.abs(delTime) > 30000) {
                return result;
            }
            String reqString = ss[0];
            reqString = reqString.replace("\\", "\\\\");
            JSONObject json = JSONObject.parseObject(reqString);
            log.info("request data：{}", json);
            return msgService.getResult(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
