package com.chenzhen.service;

import com.chenzhen.pojo.Result;

public interface MessageService {

    Result addMessage(String message);

    Result queryMessage(String flag);

    Result queryMessageById(String msgId, String flag);

    Result delMessage(String msgId);
}
