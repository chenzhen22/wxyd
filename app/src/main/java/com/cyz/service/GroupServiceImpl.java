package com.cyz.service;

import com.cyz.constant.ErrorEnum;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupServiceImpl implements GroupService {

    /** 每个用户最多可创建的群聊数 */
    private static final int MAX_GROUP_PER_USER = 5;
    /** 中转消息保留天数（长期离线成员兜底清理） */
    private static final int TRANSIT_RETAIN_DAYS = 7;
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    MysqlMapper mysqlMapper;

    @Override
    public Result create(Long userId, String name) {
        Result result = Result.getInstance();
        if (name == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000011);
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 20) {
            return result.setErrorEnum(ErrorEnum.ERROR000011);
        }
        if (mysqlMapper.countGroupsByOwner(userId) >= MAX_GROUP_PER_USER) {
            return result.setErrorEnum(ErrorEnum.ERROR000010);
        }
        // 群名重复校验
        List<Map<String, Object>> same = mysqlMapper.searchGroups(trimmed, userId);
        if (same != null && same.stream().anyMatch(g -> trimmed.equals(String.valueOf(g.get("name"))))) {
            result.setErrorEnum(ErrorEnum.ERROR000011);
            result.setErrorMsg("群名已存在");
            return result;
        }
        Map<String, Object> param = new HashMap<>(4);
        param.put("name", trimmed);
        param.put("ownerId", userId);
        mysqlMapper.insertGroup(param);
        Object idVal = param.get("id");
        long groupId = idVal instanceof Number ? ((Number) idVal).longValue() : 0L;
        mysqlMapper.insertGroupMember(groupId, userId, "owner");
        Map<String, Object> body = new HashMap<>(2);
        body.put("groupId", groupId);
        result.setBody(body);
        return result;
    }

    @Override
    public Result my(Long userId) {
        Result result = Result.getInstance();
        result.setBody(mysqlMapper.listMyGroups(userId));
        return result;
    }

    @Override
    public Result search(Long userId, String keyword) {
        Result result = Result.getInstance();
        if (keyword == null || keyword.trim().isEmpty()) {
            result.setBody(new HashMap<>());
            return result;
        }
        result.setBody(mysqlMapper.searchGroups(keyword.trim(), userId));
        return result;
    }

    @Override
    public Result apply(Long userId, Long groupId, String reason) {
        Result result = Result.getInstance();
        if (groupId == null || queryGroup(groupId) == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (reason != null && reason.length() > 200) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (mysqlMapper.countMember(groupId, userId) > 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000012);
        }
        if (mysqlMapper.countPendingApply(groupId, userId) > 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000013);
        }
        mysqlMapper.insertApply(groupId, userId, reason);
        return result;
    }

    @Override
    public Result applies(Long ownerId) {
        Result result = Result.getInstance();
        result.setBody(mysqlMapper.listPendingApplies(ownerId));
        return result;
    }

    @Override
    public Result handle(Long ownerId, Long applyId, boolean approve) {
        Result result = Result.getInstance();
        if (applyId == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        Map<String, Object> apply = mysqlMapper.queryApplyById(applyId);
        if (apply == null || !"0".equals(String.valueOf(apply.get("status")))) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        long groupId = ((Number) apply.get("groupId")).longValue();
        Map<String, Object> group = queryGroup(groupId);
        if (group == null || !ownerId.equals(((Number) group.get("ownerId")).longValue())) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        mysqlMapper.updateApplyStatus(applyId, approve ? 1 : 2);
        if (approve) {
            long applicantId = ((Number) apply.get("userId")).longValue();
            if (mysqlMapper.countMember(groupId, applicantId) == 0) {
                mysqlMapper.insertGroupMember(groupId, applicantId, "member");
            }
        }
        return result;
    }

    @Override
    public Result kick(Long ownerId, Long groupId, Long targetUserId) {
        Result result = Result.getInstance();
        Map<String, Object> group = queryGroup(groupId);
        if (group == null || targetUserId == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (!ownerId.equals(((Number) group.get("ownerId")).longValue())) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        if (targetUserId.equals(ownerId)) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (mysqlMapper.countMember(groupId, targetUserId) == 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        mysqlMapper.deleteGroupMember(groupId, targetUserId);
        // 被踢成员不再拉取，其未送达中转消息直接清理
        mysqlMapper.deleteTransitByUser(groupId, targetUserId);
        return result;
    }

    @Override
    public Result users(Long ownerId, Long groupId, String keyword) {
        Result result = Result.getInstance();
        Map<String, Object> group = queryGroup(groupId);
        if (group == null || !ownerId.equals(((Number) group.get("ownerId")).longValue())) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            result.setBody(new HashMap<>());
            return result;
        }
        result.setBody(mysqlMapper.searchUsersForInvite(keyword.trim(), groupId));
        return result;
    }

    @Override
    public Result invite(Long ownerId, Long groupId, Long targetUserId) {
        Result result = Result.getInstance();
        Map<String, Object> group = queryGroup(groupId);
        if (group == null || targetUserId == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (!ownerId.equals(((Number) group.get("ownerId")).longValue())) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        if (targetUserId.equals(ownerId) || mysqlMapper.countMember(groupId, targetUserId) > 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000012);
        }
        mysqlMapper.insertGroupMember(groupId, targetUserId, "member");
        return result;
    }

    @Override
    public Result members(Long userId, Long groupId) {
        Result result = Result.getInstance();
        if (groupId == null || mysqlMapper.countMember(groupId, userId) == 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        result.setBody(mysqlMapper.listGroupMembers(groupId));
        return result;
    }

    @Override
    public Result send(Long userId, Long groupId, String msgType, String content) {
        Result result = Result.getInstance();
        if (groupId == null || msgType == null || !("text".equals(msgType) || "emoji".equals(msgType) || "image".equals(msgType))) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (content == null || content.trim().isEmpty() || content.length() > 1000) {
            return result.setErrorEnum(ErrorEnum.ERROR000016);
        }
        if (mysqlMapper.countMember(groupId, userId) == 0) {
            return result.setErrorEnum(ErrorEnum.ERROR000014);
        }
        String expireTime = LocalDateTime.now().plusDays(TRANSIT_RETAIN_DAYS).format(DATETIME);
        List<Long> toUserIds = mysqlMapper.listMemberIds(groupId, userId);
        // 消息登记：用于已读跟踪（read_total = 发送时应达人数）
        Map<String, Object> msgParam = new HashMap<>(8);
        msgParam.put("groupId", groupId);
        msgParam.put("fromUserId", userId);
        msgParam.put("readTotal", toUserIds.size());
        msgParam.put("expireTime", expireTime);
        mysqlMapper.insertGroupMsg(msgParam);
        long msgId = msgParam.get("msgId") instanceof Number ? ((Number) msgParam.get("msgId")).longValue() : 0L;
        for (Long toUserId : toUserIds) {
            mysqlMapper.insertTransit(groupId, msgId, userId, toUserId, msgType, content, expireTime);
        }
        // 顺带清理过期中转消息与过期消息登记
        mysqlMapper.deleteExpiredTransit();
        mysqlMapper.deleteExpiredGroupMsg();
        result.setBody(msgId);
        return result;
    }

    @Override
    @Transactional
    public Result pull(Long userId) {
        Result result = Result.getInstance();
        mysqlMapper.deleteExpiredTransit();
        List<Map<String, Object>> list = mysqlMapper.pullTransitForUpdate(userId);
        if (list != null && !list.isEmpty()) {
            // 查到即删：全员拉走后服务端无残留
            List<Long> ids = list.stream()
                    .map(m -> ((Number) m.get("id")).longValue())
                    .collect(Collectors.toList());
            // 写入已读记录（去重），供发送者查询
            List<Long> msgIds = list.stream()
                    .map(m -> ((Number) m.get("msgId")).longValue())
                    .distinct()
                    .collect(Collectors.toList());
            mysqlMapper.insertMsgReads(msgIds, userId);
            mysqlMapper.deleteTransitByIds(ids);
        }
        result.setBody(list);
        return result;
    }

    @Override
    public Result readInfo(Long userId, Long groupId) {
        Result result = Result.getInstance();
        if (groupId == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        result.setBody(mysqlMapper.selectReadInfo(groupId, userId));
        return result;
    }

    @Override
    public Result readers(Long userId, Long msgId) {
        Result result = Result.getInstance();
        if (msgId == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        // SQL 内校验：查询者必须是消息发送者，否则返回空
        result.setBody(mysqlMapper.selectReaders(msgId, userId));
        return result;
    }

    private Map<String, Object> queryGroup(Long groupId) {
        return mysqlMapper.queryGroupById(groupId);
    }
}
