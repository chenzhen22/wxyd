package com.ghbank.ifp.redis.manager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ghbank.ifp.cache.CacheManager;
import com.ghbank.ifp.cache.bean.IFPCache;
import com.ghbank.ifp.redis.access.RedisClient;
import com.ghbank.ifp.redis.access.RedisKey;
import com.ghbank.ifp.redis.access.RedisManager;
import com.ghbank.ifp.redis.util.IFPCacheUtil;
import com.ghbank.ifp.util.SerializeUtils;
import com.ghbank.ifp.util.StringUtils;

public class IFPRedisManager implements CacheManager {
	
	public long timeOut = 0L;
	public String[] cacheNames;
	private static IFPRedisManager instance = new IFPRedisManager();
	private Map<String, RedisClient> cacheMap = new HashMap<>();
	private static boolean isInited = false;
	
	public synchronized static IFPRedisManager getInstance(InputStream is) {
		if(!isInited) {
			instance.init(is);
			isInited = true;
		}
		return instance;
	}
		
	public void init(InputStream is) {
		List<IFPCache> ifpCaches = IFPCacheUtil.getIfpCaches(is);

		Iterator<IFPCache> iterator = ifpCaches.iterator();
		while(iterator.hasNext()) {
			IFPCache ifpCache = (IFPCache) iterator.next();
			RedisManager redisManager = new RedisManager();
			redisManager.init(ifpCache);
			cacheMap.put(ifpCache.getId(), new RedisClient(redisManager));
		}

	}
	
	@Override
	public void put(String cacheKey, String key, Object value) throws Exception {
		this.put(cacheKey, key, value, this.timeOut);
		
	}

	@Override
	public void put(String cacheKey, String key, Object value, long timeout) throws Exception {
		RedisClient redisClient = ((RedisClient) this.cacheMap.get(cacheKey));
		RedisKey redisKey = new RedisKey(cacheKey, key);
		
		if (value instanceof String) {
			redisClient.saveString(redisKey, (String) value,timeout);
		} else if (value instanceof Map) {
			redisClient.saveMap(redisKey, (Map) value, timeout);
		} else if (value instanceof List) {
			List<Map> listValue = (List) value;
			redisClient.saveList(redisKey, listValue, timeout);
		} else {
			redisClient.saveString(redisKey, SerializeUtils.serialize(value), timeout);
		}
		
	}

	@Override
	public void remove(String cacheKey, String key) throws Exception {
		((RedisClient) this.cacheMap.get(cacheKey)).delete(new RedisKey(cacheKey, key));
	}

	@Override
	public void removeAll(String cacheKey) throws Exception {
		((RedisClient) this.cacheMap.get(cacheKey)).flushAll(cacheKey);
		
	}

	@Override
	public Object get(String cacheKey, String key) throws Exception {
		String result = ((RedisClient) this.cacheMap.get(cacheKey)).getString(new RedisKey(cacheKey, key),
				this.timeOut);
		return result == null ? null : result;
	}

	@Override
	public Object get(String cacheKey, String key, int dataType) throws Exception {
		String result = ((RedisClient) this.cacheMap.get(cacheKey)).getString(new RedisKey(cacheKey, key), this.timeOut);
		return !StringUtils.hasText(result) ? null : SerializeUtils.unSerialize(result);
	}

	@Override
	public String[] getCacheNames() {
		return this.cacheNames;
	}

	public void setCacheNames(String[] cacheNames) {
		this.cacheNames = cacheNames;
	}
	
	@Override
	public List<Object> query(String cacheKey, String sqel) throws Exception {
		return null;
	}

	@Override
	public List<Object> query(String cacheName, String sqel, int page, int count) throws Exception {
		return null;
	}

	@Override
	public int queryCount(String cacheName, String sqel) throws Exception {
		return 0;
	}

	@Override
	public List<String> getKeys(String cacheKey) {
		return new ArrayList<String>(this.getKeys(cacheKey, cacheKey));
	}

	@Override
	public List<String> getKeys(String cacheKey, String regex) {
		return new ArrayList<String>(
				((RedisClient) this.cacheMap.get(cacheKey)).getKeys(new RedisKey(cacheKey, (String) null), regex));
	}

	@Override
	public Object getCache(String cacheKey) {
		return null;
	}

	@Override
	public int removeAll(String cacheKey, String sqel) throws Exception {
		return 0;
	}

	@Override
	public Object get(String cacheKey, String key, int dataType, long timeOut) throws Exception {
		String result = ((RedisClient) cacheMap.get(cacheKey)).getString(new RedisKey(cacheKey, key), timeOut);
		return !StringUtils.hasText(result) ? null : SerializeUtils.unSerialize(result);
	}
}
