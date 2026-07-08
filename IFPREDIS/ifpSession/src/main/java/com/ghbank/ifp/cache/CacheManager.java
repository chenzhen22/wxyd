package com.ghbank.ifp.cache;

import java.util.List;

public interface CacheManager {
	
	void put(String cacheKey, String key, Object value) throws Exception;

	void put(String cacheKey, String key, Object value, long timeout) throws Exception;

	void remove(String cacheKey, String key) throws Exception;

	void removeAll(String cacheKey) throws Exception;

	Object get(String cacheKey, String key) throws Exception;

	Object get(String cacheKey, String key, int dataType) throws Exception;

	String[] getCacheNames();

	List<Object> query(String cacheKey, String sqel) throws Exception;

	List<Object> query(String cacheName, String sqel, int page, int count) throws Exception;

	int queryCount(String cacheName, String sqel) throws Exception;

	List<String> getKeys(String cacheKey);

	List<String> getKeys(String cacheKey, String regex);

	Object getCache(String cacheKey);

	int removeAll(String cacheKey, String sqel) throws Exception;

	Object get(String cacheKey, String key, int dataType, long timeOut) throws Exception;
}