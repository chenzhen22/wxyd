package com.chenzhen.service;

import com.chenzhen.pojo.OperInfo;
import com.chenzhen.pojo.Result;

import javax.servlet.http.HttpServletRequest;

public interface MessageService {

    Result addMessage(String message, HttpServletRequest request);

    Result queryMessage(String flag, HttpServletRequest request);

    Result queryMessageById(HttpServletRequest request, String msgId, String flag);

    Result queryMsgCode(String mobilePhone, String type) throws Exception;

    Result queryOperInfo();

    Result addOperInfo(OperInfo operInfo);

    Result delMessage(String msgId);
}
