package com.cyz.service;

import com.cyz.pojo.Result;

public interface MessageService {

    Result addMessage(String message);

    Result queryMessage(String flag);

    Result queryMessageById(String msgId, String flag);

    Result delMessage(String msgId);
}
