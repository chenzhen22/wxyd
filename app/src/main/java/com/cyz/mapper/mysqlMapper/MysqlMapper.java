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

	List<Map<String,Object>> queryMessage(String msgId, String requetIp);

	int queryMessageCount();

	List<Map<String,String>> queryClientInfo();

	int queryWhiteUrl(String url);

	int addLog(LogPojo logPojo);

	// ===== users =====
	int countUsers();

	int insertUser(User user);

	User queryUserByUsername(String username);

	User queryUserById(Long id);

	List<User> listUsers();

	int updateUserStatus(@Param("id") Long id, @Param("status") Integer status);

	int deleteUser(Long id);

	// ===== ding_robot =====
	int insertRobot(DingRobot robot);

	DingRobot queryRobotById(Long id);

	List<DingRobot> listRobotsByUserId(Long userId);

	int updateRobot(DingRobot robot);

	int deleteRobot(@Param("id") Long id, @Param("userId") Long userId);
}
