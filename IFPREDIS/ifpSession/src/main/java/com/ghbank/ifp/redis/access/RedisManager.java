package com.ghbank.ifp.redis.access;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghbank.ifp.cache.bean.CacheServer;
import com.ghbank.ifp.cache.bean.CacheServices;
import com.ghbank.ifp.cache.bean.IFPCache;
import com.ghbank.ifp.redis.config.HostConfig;
import com.ghbank.ifp.util.StringUtils;

import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {
	private static Logger logger = LoggerFactory.getLogger(RedisManager.class);

	private ScheduledExecutorService executorService = null;
	private boolean isInited;
	private List<RedisNode> redisNodeList = null;
	private int maxWeight = 0;
	
	public boolean init(IFPCache ifpCache) {
	      this.isInited = false;
	      if (this.redisNodeList != null) {
	         this.redisNodeList.clear();
	      }

	      if (ifpCache != null && ifpCache.isOpen()) {
	         JedisPoolConfig poolConfig = new JedisPoolConfig();
	         poolConfig.setMaxIdle(ifpCache.getMaxIdleConn());
	         poolConfig.setMinIdle(ifpCache.getMinIdleConn());
	         poolConfig.setMaxTotal(ifpCache.getMaxConn());
	         poolConfig.setMaxWaitMillis(ifpCache.getMaxWait());
	         CacheServices servers = ifpCache.getServers();
	         if (servers != null) {
	            if (this.redisNodeList == null) {
	               this.redisNodeList = new ArrayList<>();
	            }

	            int count = 1;
	            Iterator iterator = servers.getServers().iterator();

	            while(iterator.hasNext()) {
	               CacheServer ser = (CacheServer)iterator.next();
	               if (ser != null) {
	                  this.maxWeight += ser.getWeight();
	                  HostConfig master = null;
	                  if (StringUtils.hasText(ser.getMasterHost())) {
	                     master = new HostConfig(ser.getMasterHost(), ser.getMasterPost(), ser.getMasterPassword(), ser.getTimeout(), ser.getDatabases());
	                     logger.info("CACHE", 1, "添加第{}个缓存节点:{}", new Object[]{count++, ser.getMasterHost(), ser.getMasterPost()});
	                  }

	                  HostConfig salver = null;
	                  if (StringUtils.hasText(ser.getBakHost())) {
	                     salver = new HostConfig(ser.getBakHost(), ser.getBakPort(), ser.getBakPassword(), ser.getTimeout(), ser.getDatabases());
	                  }

	                  HostConfig sentinel = null;
	                  if (StringUtils.hasText(ser.getSentinelHost())) {
	                     sentinel = new HostConfig(ser.getSentinelHost(), ser.getSentinelPost(), ser.getSentinelPassword(), ser.getTimeout(), ser.getDatabases(), ser.getMasterName());
	                     logger.info("添加第{}个缓存节点:{}", new Object[]{count++, ser.getSentinelHost(), ser.getSentinelPost()});
	                  }

	                  this.redisNodeList.add(new RedisNode(poolConfig, master, salver, sentinel, ser.getWeight()));
	               }
	            }

	            if (ifpCache.getIntervalTime() != 0L) {
	               if (this.executorService != null) {
	                  this.executorService.shutdown();
	                  this.executorService = null;
	               }

	               this.executorService = new ScheduledThreadPoolExecutor(1, (new Builder()).namingPattern("redis-heart-beat-check-schedule-pool-%d").daemon(false).build());
	               this.executorService.scheduleAtFixedRate(new Runnable() {

					@Override
					public void run() {
						validate();
					}
	            	   
	               }, ifpCache.getIntervalTime(), ifpCache.getIntervalTime(), TimeUnit.MILLISECONDS);
	               logger.info("心跳检测线程已经启动...");
	            }

	            if (this.redisNodeList != null && !this.redisNodeList.isEmpty()) {
	               this.isInited = true;
	            }
	         }
	      }

	      if (!this.isInited) {
	         logger.info("初始化Redis失败，配置文件不存在或缓存开关已关闭...");
	      }

	      return this.isInited;
	   }
	
	public RedisAccess getRedisAccess(RedisKey redisKey) {
		if (!this.isInited) {
			logger.info("缓存尚未初始化...");
			return null;
		} else {
			if (this.redisNodeList != null && !this.redisNodeList.isEmpty()) {
				int nodeCount = this.redisNodeList.size();
				long hashValue = redisKey.getRedisKeyHashValue();
				int index = 0;
				int sum;
				if (this.maxWeight == 0) {
					index = (int) (hashValue % (long) nodeCount);
				} else {
					int rand = (int) (hashValue % (long) this.maxWeight);
					sum = 0;

					for (int i = 0; i < nodeCount; ++i) {
						sum += ((RedisNode) this.redisNodeList.get(i)).getWeight();
						if (rand < sum) {
							index = i;
							break;
						}
					}
				}

				RedisNode node = (RedisNode) this.redisNodeList.get(index);
				if (node.isAvailable()) {
					return node.getRedisAccess(true);
				}

				for (sum = nodeCount - 1; sum > 0; --sum) {
					++index;
					if (index == nodeCount) {
						index = 0;
					}

					node = (RedisNode) this.redisNodeList.get(index);
					if (node.isAvailable()) {
						return node.getRedisAccess(false);
					}
				}
			}

			return null;
		}
	}
	
	private void validate() {
		if (this.redisNodeList != null && !this.redisNodeList.isEmpty()) {
			Iterator iterator = this.redisNodeList.iterator();

			while (iterator.hasNext()) {
				RedisNode node = (RedisNode) iterator.next();
				node.validate();
			}
		}
	}
	
	public void flushAll() {
		if (this.redisNodeList != null && !this.redisNodeList.isEmpty()) {
			Iterator iterator = this.redisNodeList.iterator();

			while (iterator.hasNext()) {
				RedisNode node = (RedisNode) iterator.next();
				node.flushall();
			}
		}

	}
	
}
