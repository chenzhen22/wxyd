package com.ghbank.ifp.redis.access;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisClient {
	private RedisManager redisManager;

	public RedisClient() {
	}

	public RedisClient(RedisManager redisManager) {
		this.redisManager = redisManager;
	}

	public int saveString(RedisKey redisKey, String data, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.insertString(redisKey, data, timeout) : -1;
	}

	public int saveMap(RedisKey redisKey, Map<String, String> data, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.insertHashMap(redisKey, data, timeout) : -1;
	}

	public int saveMapField(RedisKey redisKey, String data, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.insertHashMapField(redisKey, redisKey.getField(), data, timeout) : -1;
	}

	public int saveList(RedisKey redisKey, List data, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.insertList(redisKey, data, timeout) : 0;
	}

	public int delete(RedisKey redisKey) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		if (ra != null) {
			String field = redisKey.getField();
			return field != null && !"".equals(field.trim())
					? ra.deleteHashMapField(redisKey, new String[]{field})
					: ra.delete(redisKey);
		} else {
			return -1;
		}
	}

	public String getString(RedisKey redisKey, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.getString(redisKey, timeout) : null;
	}

	public List getList(RedisKey redisKey, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.getList(redisKey, timeout) : null;
	}

	public Map<String, String> getMap(RedisKey redisKey, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.getHashMap(redisKey, timeout) : null;
	}

	public String getMapField(RedisKey redisKey, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.getHashMapField(redisKey, redisKey.getField(), timeout) : null;
	}

	public String getSeqNo(RedisKey redisKey, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		if (ra != null) {
			long rev = ra.getSeqNo(redisKey, timeout);
			if (rev >= 0L) {
				return String.valueOf(rev);
			}
		}

		return null;
	}

	public int updateString(RedisKey redisKey, String data, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.updateString(redisKey, data, timeout) : -1;
	}

	public int updateMap(RedisKey redisKey, Map<String, String> data, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.updateHashMap(redisKey, data, timeout) : -1;
	}

	public int updateMapField(RedisKey redisKey, String data, long timeout) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.updateHashMapField(redisKey, redisKey.getField(), data, timeout) : -1;
	}

	public Set<String> getKeys(RedisKey redisKey, String regex) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.getKeys(redisKey, regex) : null;
	}

	public List<String> getMString(RedisKey redisKey, String[] keys) {
		RedisAccess ra = this.redisManager.getRedisAccess(redisKey);
		return ra != null ? ra.getMString(redisKey, keys) : null;
	}

	public void flushAll(String redisKey) {
		this.redisManager.flushAll();
	}
}
