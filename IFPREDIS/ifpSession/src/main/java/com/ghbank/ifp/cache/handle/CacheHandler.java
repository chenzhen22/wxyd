package com.ghbank.ifp.cache.handle;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghbank.ifp.cache.CacheManager;
import com.ghbank.ifp.redis.manager.IFPRedisManager;

public class CacheHandler implements CacheManager {
	private static Logger logger = LoggerFactory.getLogger(CacheHandler.class);
	
	private static CacheHandler cacheHandler = new CacheHandler();
	private Map<String, CacheManager> cacheMap;
	private boolean initFlag = false;
	private String[] cacheNames;
	
	public static CacheHandler getInstance() {
		return cacheHandler;
	}
	
	public void init(InputStream is) {
		if (!this.initFlag) {
			IFPRedisManager ifpRedisManager = IFPRedisManager.getInstance(is);
			if (null == this.cacheMap) {
				cacheMap = new HashMap<>();
				cacheMap.put("nmb_cache_X", ifpRedisManager);
				cacheMap.put("nmb_cache_Y", ifpRedisManager);
			}

			this.initFlag = true;
		}
	}

	public Object get(String cacheName, String key) {
		try {
			return ((CacheManager) this.cacheMap.get(cacheName)).get(cacheName, key);
		} catch (Exception e) {
			logger.info("get Value from Cahce Error:", e);
		}
		return null;
	}

	public void put(String cacheName, String key, Object value) {
		try {
			((CacheManager) this.cacheMap.get(cacheName)).put(cacheName, key, value);
		} catch (Exception e) {
			logger.info("put Value to Cahce Error:", e);
		}

	}

	public void remove(String cacheName, String key) {
		try {
			((CacheManager) this.cacheMap.get(cacheName)).remove(cacheName, key);
		} catch (Exception e) {
			logger.info("remove from Cahce Error:", e);
		}

	}

	public void removeAll(String cacheName) {
		try {
			((CacheManager) this.cacheMap.get(cacheName)).removeAll(cacheName);
		} catch (Exception e) {
			logger.info("removeAll from Cahce Error:", e);
		}

	}

	public String[] getCacheNames() {
		if (null == this.cacheNames) {
			this.cacheNames = new String[this.cacheMap.size()];
			int i = 0;

			for (Iterator<String> iterator = this.cacheMap.keySet().iterator(); iterator.hasNext(); ++i) {
				String cacheName = (String) iterator.next();
				this.cacheNames[i] = cacheName;
			}
		}

		return this.cacheNames;
	}

	public Map<String, CacheManager> getCacheMap() {
		return this.cacheMap;
	}

	public List<Object> query(String cacheName, String sqel) {
		try {
			return ((CacheManager) this.cacheMap.get(cacheName)).query(cacheName, sqel);
		} catch (Exception e) {
			logger.info("query from Cahce Error:", e);
		}
		return null;
	}

	public List<Object> query(String cacheName, String sqel, int page, int count) {
		try {
			return ((CacheManager) this.cacheMap.get(cacheName)).query(cacheName, sqel, page, count);
		} catch (Exception e) {
			logger.info("query from Cahce Error:", e);
		}
		return null;
	}

	public int queryCount(String cacheName, String sqel) {
		try {
			return ((CacheManager) this.cacheMap.get(cacheName)).queryCount(cacheName, sqel);
		} catch (Exception e) {
			logger.info("query from Cahce Error:", e);
		}
		return 0;
	}

	public List<String> getKeys(String cacheName) {
		return ((CacheManager) this.cacheMap.get(cacheName)).getKeys(cacheName);
	}

	public List<String> getKeys(String cacheName, String regex) {
		return ((CacheManager) this.cacheMap.get(cacheName)).getKeys(cacheName, regex);
	}

	public Object getCache(String cacheName) {
		return ((CacheManager) this.cacheMap.get(cacheName)).getCache(cacheName);
	}

	public int removeAll(String cacheName, String sqel) {
		try {
			List<Object> queryList = this.query(cacheName, sqel);
			int count = 0;

			for (Iterator<Object> iterator = queryList.iterator(); iterator.hasNext(); ++count) {
				Object queryCache = iterator.next();
				this.remove(cacheName, ((Object[]) ((Object[]) queryCache))[0].toString());
			}

			return count;
		} catch (Exception e) {
			logger.info("removeAll from Cahce Error:", e);
		}
		return 0;
	}

	public Object get(String cacheName, String key, int valueType) {
		try {
			return ((CacheManager) this.cacheMap.get(cacheName)).get(cacheName, key, valueType);
		} catch (Exception e) {
			logger.info("get Value from Cahce Error:", e);
		}
		return null;
	}

	public void put(String cacheName, String key, Object value, long timeOut) {
		try {
			((CacheManager) this.cacheMap.get(cacheName)).put(cacheName, key, value, timeOut);
		} catch (Exception e) {
			logger.info("save Value to Cahce Error:", e);
		}

	}

	public Object get(String cacheName, String key, int valueType, long timeOut) {
		try {
			return ((CacheManager) this.cacheMap.get(cacheName)).get(cacheName, key, valueType, timeOut);
		} catch (Exception e) {
			logger.info("get Value from Cahce Error:", e);
		}
		return null;
	}
}