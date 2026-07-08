package com.chenzhen.service;

import com.chenzhen.pojo.Result;

import javax.servlet.http.HttpServletRequest;

public interface LoginService {

    Result updateToken(String userName, String Ostype, HttpServletRequest request) throws Exception;

    Result updateTokenNG(String userName, String Ostype, String lstype, String clientIp) throws Exception;

}
