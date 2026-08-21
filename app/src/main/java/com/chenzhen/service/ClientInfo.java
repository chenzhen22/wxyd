package com.chenzhen.service;

import com.chenzhen.pojo.LogPojo;

import java.util.Map;

public interface ClientInfo {
	Map<String, String> getUserName(String client);
	
	boolean checkwhite(String url);

	int addLog(LogPojo logPojo);
}
