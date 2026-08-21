package com.chenzhen.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.pojo.Result;
import org.slf4j.MDC;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MsgService {

    public static Result getResult(JSONObject json) {
        json.put("traceId", MDC.get("traceId"));
        json.put("clientIp", MDC.get("clientIp"));
        Result result = Result.getInstance();
        File file = null;
        File fileRev = null;
        PrintWriter pw = null;
        BufferedReader br = null;
        try {
            String uuid = UUID.randomUUID().toString().replace("-", "");
            File fileDir = new File("./msg/");
            if (!fileDir.exists()) {
                fileDir.mkdirs();
                try {
                    if(!System.getProperty("os.name").toLowerCase().startsWith("win")){
                        Runtime.getRuntime().exec("chmod 777 " + fileDir.getAbsolutePath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file = new File("./msg/" + uuid + ".send");
            if (!file.exists()) {
                file.createNewFile();
                try {
                    if(!System.getProperty("os.name").toLowerCase().startsWith("win")){
                        Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            pw.write(json.toString());
            pw.flush();
            int index = 0;
            while (true) {
                fileRev = new File("./msg/" + uuid + ".rev");
                if (fileRev.exists()) {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(fileRev), StandardCharsets.UTF_8));
                    String reqstr = br.readLine();
                    result =  JSON.parseObject(reqstr, Result.class);
                    return result;
                }
                if (index > 60) {
                    return result;
                }
                index++;
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != file && file.exists()) {
                file.delete();
            }
            if (null != fileRev && fileRev.exists()) {
                fileRev.delete();
            }
            if (pw != null) {
                pw.close();
            }
        }
        return result;
    }

    public static String getParamValue(String key) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", key);
        jsonObject.put("action", "getParamValue");
        return (String) getResult(jsonObject).getBody();
    }
}
