package com.ghbank.ifp.redis.config;

import java.util.HashMap;
import java.util.Map;

public class SystemConf {
	
	private static Map<String, Object> confMap = new HashMap<>();
	
	public static Map<String, Object> getConfMap() {
		return confMap;
	}
	
	public static synchronized void setConfMap(String newKey, Object newValue) {
		confMap.put(newKey, newValue);
	}

}
