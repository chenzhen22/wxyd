package com.cyz.service;

import com.cyz.pojo.Result;

public interface MessageService {

    /** 新增留言（记录 user_id），超出 50 条自动清理最旧的 */
    Result addMessage(Long userId, String message);

    /** 管理员删任意留言（按 id） */
    Result delMessage(String msgId);

    /** 普通用户只能删自己的（按 id + user_id），返回受影响行数 */
    int delMessageByUser(Long userId, String msgId);

    /** 查询留言：flag=1 只查自己的（按 user_id），否则全部 */
    Result queryMessage(Long userId, String flag);

    Result queryMessageById(String msgId, String flag);
}
