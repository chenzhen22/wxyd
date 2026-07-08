package com.ghbank.ifp.redis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghbank.ifp.base.Constants;
import com.ghbank.ifp.cache.bean.CacheServer;
import com.ghbank.ifp.cache.bean.CacheServices;
import com.ghbank.ifp.cache.bean.IFPCache;
import com.ghbank.ifp.redis.config.SystemConf;

public class IFPCacheUtil {
	private static Logger logger = LoggerFactory.getLogger(IFPCacheUtil.class);
	
	public static List<IFPCache> getIfpCaches(InputStream is) {
		List<IFPCache> ifpCaches = new ArrayList<>();
		try {
			Properties properties = new Properties();
			properties.load(is);
			String sys_idc = properties.getProperty("sys.idc","X");
			SystemConf.setConfMap(Constants.SYS_IDC_FLAG, sys_idc);
			
			String redis_intervalTime = properties.getProperty("redis.intervalTime","");
			String redis_maxIdleConn = properties.getProperty("redis.maxIdleConn","");
			String redis_minIdleConn = properties.getProperty("redis.minIdleConn","");
			String redis_maxConn = properties.getProperty("redis.maxConn","");
			String redis_maxWait = properties.getProperty("redis.maxWait","");
			String redisX_sentinelHost0 = properties.getProperty("redisX.sentinelHost.0","");
			String redisX_sentinelHost1 = properties.getProperty("redisX.sentinelHost.1","");
			String redisX_sentinelHost2 = properties.getProperty("redisX.sentinelHost.2","");
			String redisY_sentinelHost0 = properties.getProperty("redisY.sentinelHost.0","");
			String redisY_sentinelHost1 = properties.getProperty("redisY.sentinelHost.1","");
			String redisY_sentinelHost2 = properties.getProperty("redisY.sentinelHost.2","");
			String redis_sentinelPost0 = properties.getProperty("redis.sentinelPost.0","");
			String redis_sentinelPost1 = properties.getProperty("redis.sentinelPost.1","");
			String redis_sentinelPost2 = properties.getProperty("redis.sentinelPost.2","");
			String redis_masterName0 = properties.getProperty("redis.masterName.0","");
			String redis_masterName1 = properties.getProperty("redis.masterName.1","");
			String redis_masterName2 = properties.getProperty("redis.masterName.2","");
			String redis_timeout0 = properties.getProperty("redis.timeout.0","");
			String redis_timeout1 = properties.getProperty("redis.timeout.1","");
			String redis_timeout2 = properties.getProperty("redis.timeout.2","");
			String redis_redisPwd = properties.getProperty("redis.redisPwd","");
			
			IFPCache ifpCacheX = new IFPCache();
			ifpCacheX.setId("nmb_cache_X");
			ifpCacheX.setIntervalTime(redis_intervalTime);
			ifpCacheX.setMaxConn(redis_maxConn);
			ifpCacheX.setMaxIdleConn(redis_maxIdleConn);
			ifpCacheX.setMaxWait(redis_maxWait);
			ifpCacheX.setMinIdleConn(redis_minIdleConn);
			ifpCacheX.setOpen("true");
			ifpCacheX.setType("redis");
			CacheServices cacheServicesX = new CacheServices();
			List<CacheServer> serversX = new ArrayList<>();
			CacheServer cacheServerX0 = new CacheServer();
			cacheServerX0.setMasterName(redis_masterName0);
			cacheServerX0.setSentinelHost(redisX_sentinelHost0);
			cacheServerX0.setSentinelPost(redis_sentinelPost0);
			cacheServerX0.setSentinelPassword(redis_redisPwd);
			cacheServerX0.setTimeout(redis_timeout0);
			cacheServerX0.setWeight("10");
			cacheServerX0.setDatabases("-1");
			serversX.add(cacheServerX0);
			
			CacheServer cacheServerX1 = new CacheServer();
			cacheServerX1.setMasterName(redis_masterName1);
			cacheServerX1.setSentinelHost(redisX_sentinelHost1);
			cacheServerX1.setSentinelPost(redis_sentinelPost1);
			cacheServerX1.setSentinelPassword(redis_redisPwd);
			cacheServerX1.setTimeout(redis_timeout1);
			cacheServerX1.setWeight("10");
			cacheServerX1.setDatabases("-1");
			serversX.add(cacheServerX1);
			
			CacheServer cacheServerX2 = new CacheServer();
			cacheServerX2.setMasterName(redis_masterName2);
			cacheServerX2.setSentinelHost(redisX_sentinelHost2);
			cacheServerX2.setSentinelPost(redis_sentinelPost2);
			cacheServerX2.setSentinelPassword(redis_redisPwd);
			cacheServerX2.setTimeout(redis_timeout2);
			cacheServerX2.setWeight("10");
			cacheServerX2.setDatabases("-1");
			serversX.add(cacheServerX2);
			cacheServicesX.setServers(serversX);
			ifpCacheX.setServers(cacheServicesX);
			ifpCaches.add(ifpCacheX);
			
			IFPCache ifpCacheY = new IFPCache();
			ifpCacheY.setId("nmb_cache_Y");
			ifpCacheY.setIntervalTime(redis_intervalTime);
			ifpCacheY.setMaxConn(redis_maxConn);
			ifpCacheY.setMaxIdleConn(redis_maxIdleConn);
			ifpCacheY.setMaxWait(redis_maxWait);
			ifpCacheY.setMinIdleConn(redis_minIdleConn);
			ifpCacheY.setOpen("true");
			ifpCacheY.setType("redis");
			CacheServices cacheServicesY = new CacheServices();
			List<CacheServer> serversY = new ArrayList<>();
			CacheServer cacheServerY0 = new CacheServer();
			cacheServerY0.setMasterName(redis_masterName0);
			cacheServerY0.setSentinelHost(redisY_sentinelHost0);
			cacheServerY0.setSentinelPost(redis_sentinelPost0);
			cacheServerY0.setSentinelPassword(redis_redisPwd);
			cacheServerY0.setTimeout(redis_timeout0);
			cacheServerY0.setWeight("10");
			cacheServerY0.setDatabases("-1");
			serversY.add(cacheServerY0);
			
			CacheServer cacheServerY1 = new CacheServer();
			cacheServerY1.setMasterName(redis_masterName1);
			cacheServerY1.setSentinelHost(redisY_sentinelHost1);
			cacheServerY1.setSentinelPost(redis_sentinelPost1);
			cacheServerY1.setSentinelPassword(redis_redisPwd);
			cacheServerY1.setTimeout(redis_timeout1);
			cacheServerY1.setWeight("10");
			cacheServerY1.setDatabases("-1");
			serversY.add(cacheServerY1);
			
			CacheServer cacheServerY2 = new CacheServer();
			cacheServerY2.setMasterName(redis_masterName2);
			cacheServerY2.setSentinelHost(redisY_sentinelHost2);
			cacheServerY2.setSentinelPost(redis_sentinelPost2);
			cacheServerY2.setSentinelPassword(redis_redisPwd);
			cacheServerY2.setTimeout(redis_timeout2);
			cacheServerY2.setWeight("10");
			cacheServerY2.setDatabases("-1");
			serversY.add(cacheServerY2);
			cacheServicesY.setServers(serversY);
			ifpCacheY.setServers(cacheServicesY);
			ifpCaches.add(ifpCacheY);
			
		} catch (Exception e) {
			logger.info("load redis propertis is error", e);
		}
		return ifpCaches;
	}

}
