package com.cyz.mapper.mysqlMapper;

import com.cyz.pojo.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface MysqlMapper {

	int addMessage(Messages message);

	int delMessage(String msgId);

	List<Map<String,Object>> queryMessage(String msgId, String requetIp);

	int queryMessageCount();

	String getUserName(String clientIp);

	String getUserNameByStatus(String clientIp);

	List<Map<String,String>> queryClientInfo();

	int queryWhiteUrl(String url);

	int addSocketMessage(SocketMessage SocketMessage);

	String querySocketRevice(String date);

	int updateSocketMsgStatus(String date, String status);

	List<WhiteUser> queryWhiteInfo(WhiteUser whiteUser);

	int addWhite(WhiteUser whiteUser);

	int updateWhiteByUsername(WhiteUser whiteUser);

	int updateWhiteByIp(WhiteUser whiteUser);

	int deleteWhite(WhiteUser whiteUser);

	int addLog(LogPojo logPojo);
}
