package com.cyz.service;

import com.cyz.pojo.Result;

public interface GroupService {

    /** 创建群聊（每人最多 5 个），创建者自动成为管理员 */
    Result create(Long userId, String name);

    /** 我加入的群列表 */
    Result my(Long userId);

    /** 群名模糊搜索 */
    Result search(Long userId, String keyword);

    /** 申请入群 */
    Result apply(Long userId, Long groupId, String reason);

    /** 我作为管理员的待审批申请列表 */
    Result applies(Long ownerId);

    /** 审批入群申请 */
    Result handle(Long ownerId, Long applyId, boolean approve);

    /** 踢出成员（仅群主） */
    Result kick(Long ownerId, Long groupId, Long targetUserId);

    /** 邀请场景：按用户名/昵称模糊搜索用户（仅群主） */
    Result users(Long ownerId, Long groupId, String keyword);

    /** 直接拉用户入群（仅群主） */
    Result invite(Long ownerId, Long groupId, Long targetUserId);

    /** 群成员列表（须为群成员） */
    Result members(Long userId, Long groupId);

    /** 发送群消息（为每个其他成员写一行中转消息），返回消息登记 id */
    Result send(Long userId, Long groupId, String msgType, String content);

    /** 拉取并删除我的中转消息（送达即删，同时记为已读） */
    Result pull(Long userId);

    /** 我在某群发出消息的已读计数（仅发送者） */
    Result readInfo(Long userId, Long groupId);

    /** 某条消息的已读人明细（仅该消息的发送者） */
    Result readers(Long userId, Long msgId);
}
