package com.cyz.service;

import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.LogPojo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientInfoImpl implements ClientInfo {
    private static List<Map<String, String>> clientInfoList;

    @Resource
    MysqlMapper mysqlMapper;

    private void initClient() {
        clientInfoList = null;
        clientInfoList = mysqlMapper.queryClientInfo();
    }

    public Map<String, String> getUserName(String ClientIp) {
        initClient();
        Map<String, String> clientMap = new HashMap<String, String>();
        String username = "";
        for (Map<String, String> map : clientInfoList) {
            username = map.get("ip").equals(ClientIp) ? map.get("username") : "";
            if (StringUtils.hasText(username)) {
                String status = map.get("status");
                clientMap.put("username", username);
                clientMap.put("status", status);
                break;
            }
        }
        return clientMap;
    }

    @Override
    public boolean checkwhite(String url) {
        int count = mysqlMapper.queryWhiteUrl(url);
        return count > 0;
    }

    @Override
    public int addLog(LogPojo logPojo) {
        return mysqlMapper.addLog(logPojo);
    }
}
