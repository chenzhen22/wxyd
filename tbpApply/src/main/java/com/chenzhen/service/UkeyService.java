package com.chenzhen.service;

import com.chenzhen.pojo.CprUser;
import com.chenzhen.pojo.Result;

import javax.servlet.http.HttpServletRequest;

public interface UkeyService {

    Result queryUdInfo();

    CprUser queryCprUser(String userId, String type);

    Result updateUK(String userId, String type, String usbkey);

    Result udOper(HttpServletRequest request, String udStatue, String udhost);

    Result unBindUkey(String zsNumber, HttpServletRequest request) throws Exception;

    Result cfcaInfoQry(String zsNumber, HttpServletRequest request) throws Exception;
}
