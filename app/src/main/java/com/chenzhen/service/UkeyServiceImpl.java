package com.chenzhen.service;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.mapper.mysqlMapper.MysqlMapper;
import com.chenzhen.pojo.*;
import com.chenzhen.util.CommUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class UkeyServiceImpl implements UkeyService {

    @Resource
    MysqlMapper mysqlMapper;

    @Autowired
    MessageService messageService;

    // ---- Oracle-dependent methods (AtsMapperFactory/CbsMapperFactory removed) ----
    // These institution-data lookups were served by Oracle datasources that have
    // been deleted. In mock-first mode the MockAspect short-circuits the
    // corresponding tbphx.do actions before they reach here, so the bodies are
    // stubbed. If Oracle is reintroduced, restore the factory calls here.

    @Override
    public Result queryUdInfo() {
        return Result.getInstance();
    }

    @Override
    public CprUser queryCprUser(String userId, String type) {
        return null;
    }

    @Override
    public Result updateUK(String userId, String type, String usbkey) {
        return Result.getInstance();
    }

    @Override
    public Result udOper(String udStatue, String udhost) {
        return Result.getInstance();
    }

    // ---- MySQL-backed methods (kept) ----

    @Override
    public Result unBindUkey(String zsNumber) throws Exception {
        String dates = CommUtils.getDateString("yyyyMMddHHmmssS");
        SocketMessage socketMessage = new SocketMessage("4", "", zsNumber, "", "0", dates,
                CommUtils.getClientIpByMDC(), CommUtils.getHostAddress());
        socketMessage.setHandleIp(CommUtils.getParamValue("handleIp"));
        mysqlMapper.addSocketMessage(socketMessage);
        Result result = Result.getInstance();
        int index = 0;
        while (true) {
            String revice = mysqlMapper.querySocketRevice(socketMessage.getDate());
            if (StringUtils.hasText(revice)) {
                JSONObject rt = JSONObject.parseObject(revice);
                StringBuffer sbString = new StringBuffer();
                String code = rt.getString("code");
                if ("2000".equals(code)) {
                    sbString.append("解绑了证书：" + zsNumber);
                    String operTime = CommUtils.getDateString("yyyy/MM/dd hh:mm:ss");
                    String clientIp = CommUtils.getClientIpByMDC();
                    OperInfo operInfo = new OperInfo(clientIp, sbString.toString(), operTime);
                    messageService.addOperInfo(operInfo);
                }
                result.setBody(rt);
                return result;
            }

            if (index > 60) {
                mysqlMapper.updateSocketMsgStatus(dates, "1");
                return null;
            }
            index++;
            Thread.sleep(500);
        }
    }

    @Override
    public Result cfcaInfoQry(String zsNumber) throws Exception {
        Result result = Result.getInstance();
        String date = CommUtils.getDateString("yyyyMMddhhmmssS");
        SocketMessage socketMessage = new SocketMessage("3", "", zsNumber, "",
                "0", date, CommUtils.getClientIpByMDC(), CommUtils.getHostAddress());
        socketMessage.setHandleIp(CommUtils.getParamValue("handleIp"));
        mysqlMapper.addSocketMessage(socketMessage);

        int index = 0;
        while (true) {
            String revice = mysqlMapper.querySocketRevice(socketMessage.getDate());
            if (StringUtils.hasText(revice)) {
                result.setBody(JSONObject.parseObject(revice));
                return result;
            }

            if (index > 60) {
                mysqlMapper.updateSocketMsgStatus(date, "1");
                return null;
            }
            index++;
            Thread.sleep(500);
        }

    }

}
