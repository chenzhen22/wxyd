package com.chenzhen.service;

import com.alibaba.fastjson.JSONObject;
import com.chenzhen.constant.ErrorEnum;
import com.chenzhen.factory.AtsMapperFactory;
import com.chenzhen.factory.CbsMapperFactory;
import com.chenzhen.mapper.AtsMapper;
import com.chenzhen.mapper.mysqlMapper.MysqlMapper;
import com.chenzhen.pojo.*;
import com.chenzhen.util.CommUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UkeyServiceImpl implements UkeyService {

    @Resource
    AtsMapperFactory atsMapperFactory;

    @Resource
    CbsMapperFactory cbsMapperFactory;

    @Resource
    MysqlMapper mysqlMapper;

    @Autowired
    MessageService messageService;

    @Override
    public Result queryUdInfo() {
        List<UdInfo> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            list.add(UdInfo.getInstance().no(i + 1 + "").statue(cbsMapperFactory.getCbsMapper(i + "").queryUdInfo()));
        }
        Result result = Result.getInstance();
        result.setBody(list);
        return result;
    }

    @Override
    public CprUser queryCprUser(String userId, String type) {
        return cbsMapperFactory.getCbsMapper(type).queryCprUser(userId);
    }

    @Override
    public Result updateUK(String userId, String type, String usbkey) {
        Result result = Result.getInstance();

        if (!StringUtils.hasText(usbkey)) {
            return result.setErrorEnum(ErrorEnum.ERROR000003);
        }

        CprUser cprUser = queryCprUser(userId, type);
        if (StringUtils.isEmpty(cprUser)) {
            return result.setErrorEnum(ErrorEnum.ERROR000001);
        }
        String regEx = "[+,=%&<>()]";
        Pattern pattern = Pattern.compile(regEx);
        if (pattern.matcher(usbkey).find()) {//包含特殊字符
            return result.setErrorEnum(ErrorEnum.ERROR000002);
        }


        AtsMapper atsMapper = atsMapperFactory.getAtsMapper(type);
        String certdn = atsMapper.queryUKData(cprUser);
        if (!StringUtils.hasText(certdn)) {
            return result.setErrorEnum(ErrorEnum.ERROR000003);
        }

        String oneStr = certdn.substring(0, certdn.indexOf("=") + 1);
        String twoStr = certdn.substring(certdn.indexOf(","));
        certdn = oneStr + usbkey + twoStr;
        cprUser.setCertdn(certdn);
        cprUser.setUsbkey(usbkey);

        atsMapper.updateUK(cprUser);

        return result;
    }

    @Override
    public Result udOper(String udStatue, String udhost) {
        cbsMapperFactory.getCbsMapper(udhost).udOper(udStatue);

        String clientIp = CommUtils.getClientIpByMDC();
        String operTime = CommUtils.getDateString("yyyy/MM/dd hh:mm:ss");
        StringBuffer sbString = new StringBuffer();
        if ("1".equals(udStatue)) {
            sbString.append(" 关闭了证书校验");
        } else {
            sbString.append(" 打开了证书校验");
        }

        if ("0".equals(udhost)) {
            sbString.append(" SIT1");
        } else if ("1".equals(udhost)) {
            sbString.append(" SIT2");
        } else if ("2".equals(udhost)) {
            sbString.append(" UAT1");
        } else if ("3".equals(udhost)) {
            sbString.append(" UAT2");
        }
        OperInfo operInfo = new OperInfo(clientIp, sbString.toString(), operTime);
        messageService.addOperInfo(operInfo);

        return Result.getInstance();
    }

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
