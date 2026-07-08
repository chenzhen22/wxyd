package com.ghbank.ifp.redis.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghbank.ifp.redis.config.HostConfig;

public class RedisAccess {
	private static Logger logger = LoggerFactory.getLogger(RedisAccess.class);
	
	private RedisNode redisNode;
	private boolean original;

	public RedisAccess() {
	}

	public RedisAccess(RedisNode redisNode, boolean original) {
		this.redisNode = redisNode;
		this.original = original;
	}

	public int insertString(RedisKey key, String data, long timeout) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.insertString(key, data, timeout, this.original);
		} catch (Exception e) {
			logger.info("INSERT_STR@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public int insertHashMap(RedisKey key, Map<String, String> dataMap, long timeout) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.insertHashMap(key, dataMap, timeout, this.original);
		} catch (Exception e) {
			logger.info("INSERT_MAP@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public int insertList(RedisKey key, List data, long timeout) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.insertList(key, data, timeout, this.original);
		} catch (Exception e) {
			logger.info("INSERT_LIST@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public int insertHashMapField(RedisKey key, String field, String data, long timeout) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();
		StringBuffer method = new StringBuffer("INSERT_MAP_FIELD@key=");
		method.append(key).append("&field=").append(field);

		try {
			rt = this.redisNode.insertHashMapField(key, field, data, timeout, this.original);
		} catch (Exception e) {
			logger.info(method.toString(), conf.getPort() + "." + conf.getDatabases(), conf.getHost(),
					"REDIS", startTime, e);
		}

		return rt;
	}

	public int delete(RedisKey key) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.delete(key);
		} catch (Exception e) {
			logger.info("DELETE@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public int deleteHashMapField(RedisKey key, String... fields) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();
		StringBuffer method = new StringBuffer("DELETE_MAP_FIELD@key=");
		method.append(key).append("&").append("fields=").append(Arrays.toString(fields));

		try {
			rt = this.redisNode.deleteHashMapField(key, fields);
		} catch (Exception e) {
			logger.info(method.toString(), conf.getPort() + "." + conf.getDatabases(), conf.getHost(),
					"REDIS", startTime, e);
		}

		return rt;
	}

	public String getString(RedisKey key, long timeout) {
		String rt = "";
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.getString(key, timeout);
		} catch (Exception e) {
			logger.info("QUERY_STR@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public List getList(RedisKey key, long timeout) {
		List rt = Collections.EMPTY_LIST;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.getList(key, timeout);
		} catch (Exception e) {
			logger.info("QUERY_LIST@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public Map<String, String> getHashMap(RedisKey key, long timeout) {
		Map rt = Collections.EMPTY_MAP;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.getHashMap(key, timeout);
		} catch (Exception e) {
			logger.info("QUERY_MAP@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public String getHashMapField(RedisKey key, String field, long timeout) {
		String rt = "";
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();
		StringBuffer method = new StringBuffer("QUERY_MAP_FIELD@key=");
		method.append(key).append("&field=").append(field);

		try {
			rt = this.redisNode.getHashMapField(key, field, timeout);
		} catch (Exception e) {
			logger.info(method.toString(), conf.getPort() + "." + conf.getDatabases(), conf.getHost(),
					"REDIS", startTime, e);
		}

		return rt;
	}

	public long getSeqNo(RedisKey key, long timeout) {
		long rt = -1L;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.getSeqNo(key, timeout, this.original);
		} catch (Exception e) {
			logger.info("QUERY_SEQ@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public int updateString(RedisKey key, String data, long timeout) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.updateString(key, data, timeout, this.original);
		} catch (Exception e) {
			logger.info("UPDATE_STR@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public int updateHashMap(RedisKey key, Map<String, String> dataMap, long timeout) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();

		try {
			rt = this.redisNode.updateHashMap(key, dataMap, timeout, this.original);
		} catch (Exception e) {
			logger.info("UPDATE_MAP@key=" + key, conf.getPort() + "." + conf.getDatabases(),
					conf.getHost(), "REDIS", startTime, e);
		}

		return rt;
	}

	public int updateHashMapField(RedisKey key, String field, String data, long timeout) {
		int rt = -1;
		Long startTime = System.currentTimeMillis();
		HostConfig conf = this.redisNode.getConfig();
		StringBuffer method = new StringBuffer("UPDATE_MAP_FIELD@key=");
		method.append(key).append("&field=").append(field);

		try {
			rt = this.redisNode.updateHashMapField(key, field, data, timeout, this.original);
		} catch (Exception e) {
			logger.info(method.toString(), conf.getPort() + "." + conf.getDatabases(), conf.getHost(),
					"REDIS", startTime, e);
		}

		return rt;
	}

	public int flushall() {
		return this.redisNode.flushall();
	}

	public void validate() {
		this.redisNode.validate();
	}

	public Set<String> getKeys(RedisKey redisKey, String regex) {
		Set<String> rt = Collections.EMPTY_SET;
		Long startTime = System.currentTimeMillis();
		HostConfig var6 = this.redisNode.getConfig();

		try {
			rt = this.redisNode.getKeys(redisKey, regex);
		} catch (Exception e) {}

		return rt;
	}

	public List<String> getMString(RedisKey redisKey, String[] keys) {
		List<String> rt = Collections.EMPTY_LIST;
		Long startTime = System.currentTimeMillis();
		HostConfig var6 = this.redisNode.getConfig();

		try {
			rt = this.redisNode.getMString(redisKey, keys);
		} catch (Exception e) {}

		return rt;
	}
}