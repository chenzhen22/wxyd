package com.ghbank.ifp.redis.access;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghbank.ifp.redis.cmd.IRedisAccess;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class RedisPoolWraper implements IRedisAccess {
	private static Logger logger = LoggerFactory.getLogger(RedisPoolWraper.class);
	
	private static final String PONG = "PONG";
	private static final int BROKEN_LIMIT = 15;
	private static final long TIME_OUT_1M = 60000L;
	private static final long TIME_OUT_1D = 86400000L;
	private Pool pool;
	private boolean available = true;
	private int brokenCount = 0;
	private int databases = 15;

	public RedisPoolWraper(Pool pool) {
		this.pool = pool;
	}

	public RedisPoolWraper(Pool pool, int databases) {
		this.pool = pool;
		this.databases = databases;
	}

	public boolean isAvailable() {
		return this.available;
	}

	public int insertString(RedisKey redisKey, String data, long timeout, boolean original) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if ("OK".equalsIgnoreCase(jedis.set(redisKey.getRedisKey(), data))) {
				if (original) {
					if (timeout > 0L) {
						jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
					}
				} else {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(60000L));
				}

				return 1;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("字符串数据存入缓存异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1;
	}

	public int insertList(RedisKey redisKey, List<Map> data, long timeout, boolean original) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});

			int i;
			for (i = 0; i < data.size(); ++i) {
				Map mapData = (Map) data.get(i);
				String mayKey = redisKey.getRedisKey() + "$Value" + i;
				if (jedis.lpush(redisKey.getRedisKey(), new String[]{mayKey}) > 0L) {
					this.expire(jedis, redisKey.getRedisKey(), timeout, original);
					if ("OK".equalsIgnoreCase(jedis.hmset(redisKey.getRedisKey() + "$Value" + i, mapData))) {
						this.expire(jedis, mayKey, timeout, original);
					}
				}
			}

			i = data.size();
			return i;
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("字符串数据存入缓存异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1;
	}

	public int insertHashMap(RedisKey redisKey, Map<String, String> dataMap, long timeout, boolean original) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if ("OK".equalsIgnoreCase(jedis.hmset(redisKey.getRedisKey(), dataMap))) {
				if (original) {
					if (timeout > 0L) {
						jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
					}
				} else {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(60000L));
				}

				return 1;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("HashMap数据存入缓存异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1;
	}

	public int insertHashMapField(RedisKey redisKey, String field, String data, long timeout, boolean original) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if (jedis.hset(redisKey.getRedisKey(), field, data) >= 0L) {
				if (original) {
					if (timeout > 0L) {
						jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
					}
				} else {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(60000L));
				}

				return 1;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("CACHE", 3, "HashMapField数据存入缓存异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1;
	}

	public int delete(RedisKey redisKey) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if (jedis.del(redisKey.getRedisKey()) < 0L) {
				return -1;
			}

		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("删除缓存数据异常：{}", new Object[]{redisKey, e});
			return -1;
		} finally {
			this.release(jedis);
		}

		return 1;
	}

	public int deleteHashMapField(RedisKey redisKey, String... fields) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if (!jedis.exists(redisKey.getRedisKey()) || jedis.hdel(redisKey.getRedisKey(), fields) >= 0L) {
				return 1;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("从缓存删除HashMapField数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1;
	}

	public String getString(RedisKey redisKey, long timeout) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			String revStr = jedis.get(redisKey.getRedisKey());
			if (revStr != null && !"".equals(revStr)) {
				if (timeout > 0L) {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
				}

				return revStr;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("从缓存获取字符串数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return null;
	}

	public List getList(RedisKey redisKey, long timeout) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			List<Map> revList = new ArrayList();
			List<String> mapkeyList = jedis.lrange(redisKey.getRedisKey(), 0L, -1L);
			Iterator iterator = mapkeyList.iterator();

			while (iterator.hasNext()) {
				String mapKey = (String) iterator.next();
				revList.add(jedis.hgetAll(mapKey));
				if (timeout > 0L) {
					jedis.expire(mapKey, this.procTimeout(timeout));
				}
			}

			if (mapkeyList.size() > 0 && timeout > 0L) {
				jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
			}

			if (revList.size() >= 1) {
				return revList;
			}

			return null;
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("从缓存获取字符串数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return null;
	}

	public Map<String, String> getHashMap(RedisKey redisKey, long timeout) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			Map<String, String> revMap = jedis.hgetAll(redisKey.getRedisKey());
			if (revMap != null && !revMap.isEmpty()) {
				if (timeout > 0L) {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
				}

				return revMap;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("从缓存获取HashMap数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return null;
	}

	public String getHashMapField(RedisKey redisKey, String field, long timeout) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			String revStr = jedis.hget(redisKey.getRedisKey(), field);
			if (revStr != null && !"".equals(revStr)) {
				if (timeout > 0L) {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
				}

				return revStr;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("从缓存获取HashMapField数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return null;
	}

	public long getSeqNo(RedisKey redisKey, long timeout, boolean original) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if (jedis.exists(redisKey.getRedisKey()) && jedis.ttl(redisKey.getRedisKey()) < 0L) {
				if (original) {
					if (timeout > 0L) {
						jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
					} else {
						jedis.expire(redisKey.getRedisKey(), this.procTimeout(86400000L));
					}
				} else {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(60000L));
				}
			}

			long incr = jedis.incr(redisKey.getRedisKey());
			return incr;
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("CACHE", 3, "从缓存获取可增长序号数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1L;
	}

	public int updateString(RedisKey redisKey, String data, long timeout, boolean original) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("CACHE", 0, "选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if (jedis.exists(redisKey.getRedisKey())
					&& "OK".equalsIgnoreCase(jedis.set(redisKey.getRedisKey(), data))) {
				if (timeout > 0L) {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
				}

				return 1;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("CACHE", 3, "缓存更新字符串数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1;
	}

	public int updateHashMap(RedisKey redisKey, Map<String, String> dataMap, long timeout, boolean original) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if (jedis.exists(redisKey.getRedisKey())
					&& "OK".equalsIgnoreCase(jedis.hmset(redisKey.getRedisKey(), dataMap))) {
				if (timeout > 0L) {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
				}

				return 1;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("缓存更新HashMap数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1;
	}

	public int updateHashMapField(RedisKey redisKey, String field, String data, long timeout, boolean original) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			if (jedis.exists(redisKey.getRedisKey()) && jedis.hset(redisKey.getRedisKey(), field, data) >= 0L) {
				if (timeout > 0L) {
					jedis.expire(redisKey.getRedisKey(), this.procTimeout(timeout));
				}

				return 1;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("缓存更新HashMapField数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return -1;
	}

	public void validate() {
		int i = 5;

		for (int k = 0; k < i; ++k) {
			if (this.checkPoolAvailable()) {
				if (!this.available) {
					this.available = true;
				}

				if (this.brokenCount != 0) {
					this.brokenCount = 0;
				}
				break;
			}

			if (this.brokenCount < 15) {
				++this.brokenCount;
			}

			if (this.brokenCount == 15 && this.available) {
				this.available = false;
			}
		}

	}

	public int flushall() {
		Jedis jedis = null;

		byte var2;
		try {
			jedis = this.getResource();
			if (!"OK".equalsIgnoreCase(jedis.flushAll())) {
				return -1;
			}

		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("清空缓存数据异：", e);
			return -1;
		} finally {
			this.release(jedis);
		}

		return 1;
	}

	private boolean checkPoolAvailable() {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			if (!"PONG".equals(jedis.ping())) {
				return false;
			}

		} catch (Exception e) {
			this.disconnect(jedis);
			return false;
		} finally {
			this.release(jedis);
		}

		return true;
	}

	private int procTimeout(long timeout) {
		int time = (int) (timeout / 1000L);
		return time == 0 ? 1 : time;
	}

	private void disconnect(Jedis jedis) {
		try {
			jedis.disconnect();
		} catch (Exception e) {
			logger.info("jedis disconnect error:", e);
		}

	}

	private void release(Jedis jedis) {
		if (jedis != null) {
			try {
				this.pool.returnResource(jedis);
			} catch (Exception var6) {
				try {
					jedis.disconnect();
				} catch (Exception e) {
					logger.info("release jedis disconnect error:", e);
				}

				try {
					this.pool.returnBrokenResource(jedis);
				} catch (Exception e) {
					logger.info("release jedis returnBrokenResource error:", e);
				}
			}
		}

	}

	private Jedis getResource() {
		return (Jedis) this.pool.getResource();
	}

	public void expire(Jedis jedis, String redisKey, long timeout, boolean original) {
		if (original) {
			if (timeout > 0L) {
				jedis.expire(redisKey, this.procTimeout(timeout));
			} else {
				jedis.expire(redisKey, this.procTimeout(86400000L));
			}
		} else {
			jedis.expire(redisKey, this.procTimeout(60000L));
		}

	}

	public Set<String> getKeys(RedisKey redisKey, String regex) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			Set<String> keys = jedis.keys(regex);
			if (keys != null && keys.size() != 0) {
				return keys;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("从缓存获取keys数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return null;
	}

	public List<String> getMString(RedisKey redisKey, String[] keys) {
		Jedis jedis = null;

		try {
			jedis = this.getResource();
			String srev = jedis.select(redisKey.getDBIndex(this.databases));
			logger.info("选择数据库：{}->{}", new Object[]{redisKey.getDBIndex(this.databases), srev});
			List<String> values = jedis.mget(keys);
			if (values != null && values.size() != 0) {
				return values;
			}
		} catch (Exception e) {
			this.disconnect(jedis);
			logger.info("从缓存获取mvalues数据异常：{}", new Object[]{redisKey, e});
		} finally {
			this.release(jedis);
		}

		return null;
	}
}
