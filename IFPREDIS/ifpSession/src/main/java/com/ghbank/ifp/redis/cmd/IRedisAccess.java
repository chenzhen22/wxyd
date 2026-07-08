package com.ghbank.ifp.redis.cmd;

import java.util.Map;

import com.ghbank.ifp.redis.access.RedisKey;

public interface IRedisAccess {
	
	int insertString(RedisKey var1, String var2, long var3, boolean var5);

	int insertHashMap(RedisKey var1, Map<String, String> var2, long var3, boolean var5);

	int insertHashMapField(RedisKey var1, String var2, String var3, long var4, boolean var6);

	int delete(RedisKey var1);

	int deleteHashMapField(RedisKey var1, String... var2);

	String getString(RedisKey var1, long var2);

	Map<String, String> getHashMap(RedisKey var1, long var2);

	String getHashMapField(RedisKey var1, String var2, long var3);

	long getSeqNo(RedisKey var1, long var2, boolean var4);

	int updateString(RedisKey var1, String var2, long var3, boolean var5);

	int updateHashMap(RedisKey var1, Map<String, String> var2, long var3, boolean var5);

	int updateHashMapField(RedisKey var1, String var2, String var3, long var4, boolean var6);

	int flushall();
}
