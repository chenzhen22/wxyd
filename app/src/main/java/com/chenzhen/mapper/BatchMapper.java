package com.chenzhen.mapper;

import com.chenzhen.pojo.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BatchMapper {

	int updateSocketMessage(Message message);

	List<Message> queryMessageList();

	int deleteSocketMessage();

	int deleteRequestlog();
}
