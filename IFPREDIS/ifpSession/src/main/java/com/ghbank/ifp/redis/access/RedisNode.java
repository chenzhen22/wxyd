package com.ghbank.ifp.redis.access;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghbank.ifp.redis.config.HostConfig;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;

public class RedisNode {
	private static Logger logger = LoggerFactory.getLogger(RedisNode.class);
	
	
	private ECID ecid = new ECID();
	private RedisPoolWraper masterPoolWraper;
	private RedisPoolWraper slavePoolWraper;
	private HostConfig config;
	private int weight;

	public RedisNode(GenericObjectPoolConfig poolConfig, HostConfig masterConfig, HostConfig slaveConfig,
			HostConfig sentinelConfig, int weight) {
		this.weight = weight;
		JedisPool spool;
		if (sentinelConfig != null) {
			this.config = sentinelConfig;
			Set sentinels = new HashSet();
			sentinels.add(sentinelConfig.toString());
			JedisSentinelPool sentinelPool = new JedisSentinelPool(sentinelConfig.getMasterName(), sentinels,
					poolConfig, sentinelConfig.getTimeout(), sentinelConfig.getPassword());
			this.masterPoolWraper = new RedisPoolWraper(sentinelPool, sentinelConfig.getDatabases());
		} else {
			this.config = masterConfig;
			spool = new JedisPool(poolConfig, masterConfig.getHost(), masterConfig.getPort(), masterConfig.getTimeout(),
					masterConfig.getPassword());
			this.masterPoolWraper = new RedisPoolWraper(spool, masterConfig.getDatabases());
		}

		if (slaveConfig != null) {
			spool = new JedisPool(poolConfig, slaveConfig.getHost(), slaveConfig.getPort(), slaveConfig.getTimeout(),
					slaveConfig.getPassword());
			this.slavePoolWraper = new RedisPoolWraper(spool, masterConfig.getDatabases());
		}

	}

	public int getWeight() {
		return this.weight;
	}

	public boolean isAvailable() {
		return (this.masterPoolWraper.isAvailable()
				|| this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) && !this.ecid.isCleanable();
	}

	public RedisAccess getRedisAccess(boolean original) {
		if (this.ecid.isCleanable()) {
			logger.debug("节点故障，等待清理节点数据...");
			return null;
		} else {
			return new RedisAccess(this, original);
		}
	}

	public int insertString(RedisKey key, String data, long timeout, boolean original) {
		logger.debug("字符串数据存入缓存：key={},data={},timeout={},original={}",
				new Object[]{key, data, timeout, original});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.insertString(key, data, timeout, original);
		}

		logger.debug("字符串数据存入缓存：result={}", new Object[]{rev});
		return rev;
	}

	public int insertList(RedisKey key, List data, long timeout, boolean original) {
		logger.debug("集合数据存入缓存：key={},data={},timeout={},original={}",
				new Object[]{key, data, timeout, original});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.insertList(key, data, timeout, original);
		}

		logger.debug("集合数据存入缓存：result={}", new Object[]{rev});
		return rev;
	}

	public int insertHashMap(RedisKey key, Map<String, String> dataMap, long timeout, boolean original) {
		logger.debug("HashMap数据存入缓存：key={},data={},timeout={},original={}",
				new Object[]{key, dataMap, timeout, original});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.insertHashMap(key, dataMap, timeout, original);
		}

		logger.debug("HashMap数据存入缓存：result={}", new Object[]{rev});
		return rev;
	}

	public int insertHashMapField(RedisKey key, String field, String data, long timeout, boolean original) {
		logger.debug("HashMap-Field数据存入缓存：key={},field={},data={},timeout={},original={}",
				new Object[]{key, field, data, timeout, original});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.insertHashMapField(key, field, data, timeout, original);
		}

		logger.debug("HashMap-Field数据存入缓存：result={}", new Object[]{rev});
		return rev;
	}

	public int delete(RedisKey key) {
		logger.debug("从缓存删除数据：key={}", new Object[]{key});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.delete(key);
		}

		if (rev == -1) {
			this.addErrCache(key);
			if (this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
				this.slavePoolWraper.delete(key);
			}
		}

		logger.debug("从缓存删除数据：result={}", new Object[]{rev});
		return rev;
	}

	public int deleteHashMapField(RedisKey key, String... fields) {
		logger.debug("从缓存删除HashMap-Field数据：key={},fields={}", new Object[]{key, fields});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.deleteHashMapField(key, fields);
		}

		if (rev == -1) {
			this.addErrCache(key);
			if (this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
				this.slavePoolWraper.delete(key);
			}
		}

		logger.debug("从缓存删除HashMap-Field数据：result={}", new Object[]{rev});
		return rev;
	}

	public String getString(RedisKey key, long timeout) {
		logger.debug("从缓获取字符串数据：key={},timeout={}", new Object[]{key, timeout});
		String rev = null;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.getString(key, timeout);
		}

		if ((null == rev || "".equals(rev)) && this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
			rev = this.slavePoolWraper.getString(key, timeout);
		}

		if (null != rev && rev.length() < 1000) {
			logger.debug("从缓获取字符串数据：result={}", new Object[]{rev});
		} else if (null != rev) {
			logger.debug("从缓获取字符串数据：result={}{}", new Object[]{rev.substring(0, 1000), "......"});
		}

		return rev;
	}

	public List getList(RedisKey key, long timeout) {
		logger.debug("从缓获取List数据：key={},timeout={}", new Object[]{key, timeout});
		List<String> rev = null;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.getList(key, timeout);
		}

		if ((rev == null || "".equals(rev)) && this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
			rev = this.slavePoolWraper.getList(key, timeout);
		}

		if (rev.size() < 10) {
			logger.debug("从缓获取List数据：result={}", new Object[]{rev});
		} else {
			logger.debug("从缓获取List数据大小：size={}", new Object[]{rev.size()});
		}

		return rev;
	}

	public Map<String, String> getHashMap(RedisKey key, long timeout) {
		logger.debug("从缓获取HashMap数据：key={},timeout={}", new Object[]{key, timeout});
		Map<String, String> rev = null;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.getHashMap(key, timeout);
		}

		if ((rev == null || rev.isEmpty()) && this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
			rev = this.slavePoolWraper.getHashMap(key, timeout);
		}

		logger.debug("从缓获取HashMap数据：result={}", new Object[]{rev});
		return rev;
	}

	public String getHashMapField(RedisKey key, String field, long timeout) {
		logger.debug("从缓获取HashMap-Field数据：key={},field={},timeout={}", new Object[]{key, field, timeout});
		String rev = null;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.getHashMapField(key, field, timeout);
		}

		if ((rev == null || "".equals(rev)) && this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
			rev = this.slavePoolWraper.getHashMapField(key, field, timeout);
		}

		logger.debug("从缓获取HashMap-Field数据：result={}", new Object[]{rev});
		return rev;
	}

	public long getSeqNo(RedisKey key, long timeout, boolean original) {
		logger.debug("从缓获取序号数据：key={},original={},timeout={}", new Object[]{key, original, timeout});
		long rev = -1L;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.getSeqNo(key, timeout, original);
		}

		if (rev < 0L && this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
			rev = this.slavePoolWraper.getSeqNo(key, timeout, original);
		}

		logger.debug("从缓获取序号数据：result={}", new Object[]{rev});
		return rev;
	}

	public int updateString(RedisKey key, String data, long timeout, boolean original) {
		logger.debug("更新缓存字符串数据：key={},data={},original={},timeout={}",
				new Object[]{key, data, original, timeout});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.updateString(key, data, timeout, original);
		}

		if (rev == -1) {
			this.delete(key);
		}

		logger.debug("更新缓存字符串数据：result={}", new Object[]{rev});
		return rev;
	}

	public int updateHashMap(RedisKey key, Map<String, String> dataMap, long timeout, boolean original) {
		logger.debug("更新缓存HashMap数据：key={},data={},original={},timeout={}",
				new Object[]{key, dataMap, original, timeout});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.updateHashMap(key, dataMap, timeout, original);
		}

		if (rev == -1) {
			this.delete(key);
		}

		logger.debug("更新缓存HashMap数据：result={}", new Object[]{rev});
		return rev;
	}

	public int updateHashMapField(RedisKey key, String field, String data, long timeout, boolean original) {
		logger.debug("更新缓存HashMap-Field数据：key={},field={},data={},original={},timeout={}",
				new Object[]{key, field, data, original, timeout});
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.updateHashMapField(key, field, data, timeout, original);
		}

		if (rev == -1) {
			this.delete(key);
		}

		logger.debug("更新缓存HashMap-Field数据：result={}", new Object[]{rev});
		return rev;
	}

	public int flushall() {
		logger.debug("清空缓存数据...");
		int rev = -1;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.flushall();
		}

		if (rev == -1 && this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
			rev = this.slavePoolWraper.flushall();
		}

		logger.debug("清空缓存数据:result={}", new Object[]{rev});
		return rev;
	}

	public void validate() {
		if (this.masterPoolWraper != null) {
			this.masterPoolWraper.validate();
		}

		if (this.slavePoolWraper != null) {
			this.slavePoolWraper.validate();
		}

		if (this.masterPoolWraper.isAvailable()) {
			if (this.ecid.isCleanable()) {
				if (this.flushall() == 1) {
					this.ecid.clean();
				}
			} else {
				RedisKey key = null;

				while (!this.ecid.isCleanable() && (key = this.ecid.next()) != null) {
					this.delete(key);
				}
			}
		}

	}

	public HostConfig getConfig() {
		return this.config;
	}

	public void setConfig(HostConfig config) {
		this.config = config;
	}

	private void addErrCache(RedisKey key) {
		this.ecid.add(key);
	}

	public Set<String> getKeys(RedisKey key, String regex) {
		logger.debug("从缓获取Keys数据：regex={}", new Object[]{regex});
		Set<String> rev = null;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.getKeys(key, regex);
		}

		if ((rev == null || rev.isEmpty()) && this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
			rev = this.slavePoolWraper.getKeys(key, regex);
		}

		String keyStr = Arrays.toString(rev.toArray(new String[0]));
		if (keyStr.length() < 1000) {
			logger.debug("从缓获取Keys数据：result={}", new Object[]{keyStr});
		} else {
			logger.debug("从缓获取Keys数据：result={}", new Object[]{keyStr.substring(0, 1000), "......"});
		}

		return rev;
	}

	public List<String> getMString(RedisKey key, String[] keys) {
		String keyStr = Arrays.toString(keys);
		if (keyStr.length() < 1000) {
			logger.debug("从缓获取mString数据：keys={}", new Object[]{keyStr});
		} else {
			logger.debug("从缓获取mString数据：keys={}{}", new Object[]{keyStr.substring(0, 1000), "......"});
		}

		List<String> rev = null;
		if (this.masterPoolWraper.isAvailable()) {
			rev = this.masterPoolWraper.getMString(key, keys);
		}

		if ((rev == null || rev.isEmpty()) && this.slavePoolWraper != null && this.slavePoolWraper.isAvailable()) {
			rev = this.slavePoolWraper.getMString(key, keys);
		}

		logger.debug("从缓获取mString数据：size={}", new Object[]{rev.size()});
		return rev;
	}
}
