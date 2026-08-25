package com.cyz.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.*;
import com.cyz.util.CommUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoginServiceImpl implements LoginService {

    @Resource
    MysqlMapper mysqlMapper;

    @Override
    public Result updateToken(String userName, String Ostype) throws Exception {
        Result result = Result.getInstance();
        String clientIp = CommUtils.getClientIpByMDC();
        SocketMessage socketMessage = null;
        String ip = "";
        if ("0".equals(Ostype)) {
            ip = CommUtils.getParamValue("transBankSIT1");
            socketMessage = HttpRequestCPR09003.sendMsg(userName, "TBPSIT1");
        } else if ("1".equals(Ostype)) {
            ip = CommUtils.getParamValue("transBankSIT2");
            socketMessage = HttpRequestCPR09003.sendMsg(userName, "TBPSIT2");
        } else if ("2".equals(Ostype)) {
            ip = CommUtils.getParamValue("transBankUAT1");
            socketMessage = HttpRequestCPR09003.sendMsg(userName, "TBPUAT1");
        } else if ("3".equals(Ostype)) {
            ip = CommUtils.getParamValue("transBankUAT2");
            socketMessage = HttpRequestCPR09003.sendMsg(userName, "TBPUAT2");
        } else if ("4".equals(Ostype)) {
            ip = CommUtils.getParamValue("transBankDEV");
            socketMessage = HttpRequestCPR09003.sendMsg(userName, "TBPDEV");
        }
        socketMessage.setClientIp(clientIp);
        socketMessage.setHandleIp(CommUtils.getParamValue("handleIp"));
        mysqlMapper.addSocketMessage(socketMessage);

        int index = 0;
        while (true) {
            String revice = mysqlMapper.querySocketRevice(socketMessage.getDate());
            if (StringUtils.hasText(revice)) {
                Document doc = DocumentHelper.parseText(revice);
                JSONObject json = new JSONObject();
                HttpRequestCPR09003.dom4j2Json(doc.getRootElement(), json);
                String body = json.getString("body");
                JSONObject bodyjSON = (JSONObject) JSON.parse(body);
                String token = bodyjSON.getString("token");
                String returnUrl = "http://" + ip + ":8090/eweb/transBank/passTokenAndSentURLAction.do?LOGINTOKEN=SCF" + token + "&USERID=" + userName + "&appLoginType=4.0";
                if ("localhost".equals(ip)) {
                    returnUrl = "http://" + ip + ":8091/eweb/transBank/passTokenAndSentURLAction.do?LOGINTOKEN=SCF" + token + "&USERID=" + userName + "&appLoginType=4.0";
                }
                Map<String, String> map = new HashMap<>();
                map.put("resultHtml", returnUrl);
                result.setBody(map);
                return result;
            }

            if (index > 60) {
                mysqlMapper.updateSocketMsgStatus(socketMessage.getDate(), "1");
                return result;
            }
            index++;
            Thread.sleep(500);
        }
    }

    public String getEsbIpbyType(String Ostype) {
        String ip = "";
        if ("0".equals(Ostype)) {
            ip = CommUtils.getParamValue("ESBSIT1");
        } else if ("1".equals(Ostype)) {
            ip = CommUtils.getParamValue("ESBSIT2");
        } else if ("2".equals(Ostype)) {
            ip = CommUtils.getParamValue("ESBUAT1");
        } else if ("3".equals(Ostype)) {
            ip = CommUtils.getParamValue("ESBUAT2");
        } else if ("4".equals(Ostype)) {
            ip = CommUtils.getParamValue("ESBDEV");
        }
        return ip;
    }

    @Override
    public Result updateTokenNG(String userName, String Ostype, String lstype, String clientIp) throws Exception {
        Result result = Result.getInstance();
        String date = CommUtils.getDateString("yyyyMMddHHmmssS");
        String targetUrl = "";
        String ip = getEsbIpbyType(Ostype);
        if ("0".equals(Ostype)) {
            targetUrl = "http://"+CommUtils.getParamValue("TBPSIT1")+":7718/TBPinnerManagement/loginActionTBP.do?employeeId=" + userName + "&token=";
        } else if ("1".equals(Ostype)) {
            targetUrl = "http://"+CommUtils.getParamValue("TBPSIT2")+":7718/TBPinnerManagement/loginActionTBP.do?employeeId=" + userName + "&token=";
        } else if ("2".equals(Ostype)) {
            targetUrl = "http://"+CommUtils.getParamValue("TBPUAT1")+":7718/TBPinnerManagement/loginActionTBP.do?employeeId=" + userName + "&token=";
        } else if ("3".equals(Ostype)) {
            targetUrl = "http://"+CommUtils.getParamValue("TBPUAT2")+":7718/TBPinnerManagement/loginActionTBP.do?employeeId=" + userName + "&token=";
        } else if ("4".equals(Ostype)) {
            targetUrl = "http://"+CommUtils.getParamValue("TBPDEV")+":7718/TBPinnerManagement/loginActionTBP.do?employeeId=" + userName + "&token=";
        } else if ("5".equals(Ostype)) {
            ip = getEsbIpbyType(lstype);
            targetUrl = "http://localhost:7718/TBPinnerManagement/loginActionTBP.do?employeeId=" + userName + "&token=";
        }

        String sysId = "TBM";
        String transGlobalFlow = "G" + sysId + "xxxxx" +
                CommUtils.getDateRandomString("yyyyMMddhhmmss", 10);
        String globalSeqNo = transGlobalFlow + sysId + CommUtils.getRandomString(6);

        ReqSvcHeader reqSvcHeader = new ReqSvcHeader();
        reqSvcHeader.setConsumerId(sysId);
        reqSvcHeader.setTranCode("001007");
        reqSvcHeader.setTranSeqNo(transGlobalFlow);
        reqSvcHeader.setGlobalSeqNo(globalSeqNo);
        reqSvcHeader.setTranDate(CommUtils.getDateString("yyyyMMdd"));
        reqSvcHeader.setTranTime(CommUtils.getDateString("hhmmssS"));
        reqSvcHeader.setTranTellerNo("M00001");
        reqSvcHeader.setBranchId("800001");
        reqSvcHeader.setAcctDate(CommUtils.getDateString("yyyyMMdd"));
        reqSvcHeader.setSourceSysId(sysId);
        reqSvcHeader.setChannel("302009");
        reqSvcHeader.setUNIQUE_SEQ_NUM(transGlobalFlow);
        reqSvcHeader.setSRC_MODULE("b00001");

        UUS1007T uus1007T = new UUS1007T();
        uus1007T.setVER("1.0");
        uus1007T.setAPP(sysId);
        uus1007T.setSysId("000001");
        uus1007T.setSubUnitno(sysId + "S");
        uus1007T.setTemplateCode("801001");
        uus1007T.setDomainId(userName);

        ESBPojo pojo = new ESBPojo();
        pojo.setReqSvcHeader(reqSvcHeader);
        pojo.setSvcBody(uus1007T);
        pojo.setEsbServiceName("UUS1007T");

        String json = JSON.toJSONString(pojo);
        String message = CommUtils.json2xml(json);

        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setDate(date);
        socketMessage.setUrl(ip + ":9807");
        socketMessage.setMessage(message);
        socketMessage.setType("5");
        socketMessage.setClientIp(clientIp);
        socketMessage.setHandleIp(CommUtils.getParamValue("handleIp"));
        mysqlMapper.addSocketMessage(socketMessage);

        int index = 0;
        while (true) {
            String revice = mysqlMapper.querySocketRevice(socketMessage.getDate());
            if (StringUtils.hasText(revice)) {
                Document doc = DocumentHelper.parseText(revice);
                JSONObject json2 = new JSONObject();
                HttpRequestCPR09003.dom4j2Json(doc.getRootElement(), json2);
                JSONObject rspSvcHeader = json2.getJSONObject("RspSvcHeader");
                String returnCode = rspSvcHeader.getString("returnCode");
                String returnMsg = rspSvcHeader.getString("returnMsg");
                if(!"0".equals(CommUtils.handelErrorCode(returnCode))) {
                    result.setErrorCode(returnCode);
                    result.setErrorMsg(returnMsg);
                    return result;
                }
                String body = json2.getString("SvcBody");
                JSONObject bodyjSON = (JSONObject) JSON.parse(body);
                String token = bodyjSON.getString("token");
                String returnUrl = targetUrl + token;
                Map<String, String> map = new HashMap<>();
                map.put("returnUrl", returnUrl);
                result.setBody(map);
                return result;
            }

            if (index > 60) {
                mysqlMapper.updateSocketMsgStatus(socketMessage.getDate(), "1");
                return null;
            }
            index++;
            Thread.sleep(500);
        }
    }
}
