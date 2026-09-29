package com.cyz.mapper.mysqlMapper;

import com.cyz.pojo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MysqlMapper {

	int addMessage(Messages message);

	int delMessage(String msgId);

	int delMessageByUser(@Param("userId") Long userId, @Param("msgId") String msgId);

	List<Map<String,Object>> queryMessage(@Param("msgId") String msgId, @Param("userId") Long userId);

	int queryMessageCount();

	int trimMessages();

	List<Map<String,String>> queryClientInfo();

	int queryWhiteUrl(String url);

	int addLog(LogPojo logPojo);

	// ===== users =====
	int countUsers();

	int insertUser(User user);

	User queryUserByUsername(String username);

	User queryUserById(Long id);

	User queryUserPwdById(Long id);

	List<User> listUsers();

	int updateUserStatus(@Param("id") Long id, @Param("status") Integer status);

	int updateUserPassword(@Param("id") Long id, @Param("password") String password);

	int updateDisplayName(@Param("id") Long id, @Param("displayName") String displayName);

	int updateUserTheme(@Param("id") Long id, @Param("theme") String theme);

	String selectThemeById(Long id);

	int deleteUser(Long id);

	// ===== ding_robot =====
	int insertRobot(DingRobot robot);

	DingRobot queryRobotById(Long id);

	List<DingRobot> listRobotsByUserId(Long userId);

	int updateRobot(DingRobot robot);

	int deleteRobot(@Param("id") Long id, @Param("userId") Long userId);

	int deleteRobotByUserId(@Param("userId") Long userId);

	// ===== group_chat =====
	/** 插入群聊；param 含 name/ownerId，生成的群ID回填到 param 的 "id" */
	int insertGroup(Map<String, Object> param);

	int countGroupsByOwner(Long ownerId);

	Map<String, Object> queryGroupById(Long groupId);

	List<Map<String, Object>> listMyGroups(Long userId);

	List<Map<String, Object>> searchGroups(@Param("keyword") String keyword, @Param("userId") Long userId);

	// ===== group_member =====
	int insertGroupMember(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("role") String role);

	int countMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

	List<Map<String, Object>> listGroupMembers(Long groupId);

	List<Long> listMemberIds(@Param("groupId") Long groupId, @Param("excludeUserId") Long excludeUserId);

	int deleteGroupMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

	// ===== group_join_apply =====
	int insertApply(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("reason") String reason);

	Map<String, Object> queryApplyById(Long applyId);

	int countPendingApply(@Param("groupId") Long groupId, @Param("userId") Long userId);

	List<Map<String, Object>> listPendingApplies(Long ownerId);

	int updateApplyStatus(@Param("id") Long id, @Param("status") Integer status);

	// ===== group_msg_transit =====
	int insertTransit(@Param("groupId") Long groupId, @Param("fromUserId") Long fromUserId,
					  @Param("toUserId") Long toUserId, @Param("msgType") String msgType,
					  @Param("content") String content, @Param("expireTime") String expireTime);

	List<Map<String, Object>> pullTransitForUpdate(Long userId);

	int deleteTransitByIds(@Param("ids") List<Long> ids);

	int deleteTransitByUser(@Param("groupId") Long groupId, @Param("toUserId") Long toUserId);

	int deleteExpiredTransit();
}
