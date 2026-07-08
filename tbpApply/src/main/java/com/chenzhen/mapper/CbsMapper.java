package com.chenzhen.mapper;

import com.chenzhen.pojo.CprUser;

public interface CbsMapper {

    CprUser queryCprUser(String userId);

    int udOper(String udStatue);

    String queryUdInfo();

    int updateToken(String userName, String updateTime, String token);
}
