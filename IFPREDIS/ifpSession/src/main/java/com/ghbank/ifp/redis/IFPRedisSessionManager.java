package com.ghbank.ifp.redis;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghbank.ifp.base.Constants;
import com.ghbank.ifp.cache.CacheManager;
import com.ghbank.ifp.cache.handle.CacheHandler;
import com.ghbank.ifp.data.SessionElement;
import com.ghbank.ifp.data.SessionNameElement;
import com.ghbank.ifp.encrypt.EncryptHandle;
import com.ghbank.ifp.pool.ScheduledThreadPool;
import com.ghbank.ifp.redis.config.SystemConf;
import com.ghbank.ifp.session.IFPSession;
import com.ghbank.ifp.session.Session;
import com.ghbank.ifp.session.SessionInfo;
import com.ghbank.ifp.session.SessionManager;
import com.ghbank.ifp.session.UserSessionInfo;
import com.ghbank.ifp.util.StringUtils;

/**
 * 
 * redis会话类
 *
 */
public class IFPRedisSessionManager implements SessionManager {
	
	private static Logger logger = LoggerFactory.getLogger(IFPRedisSessionManager.class);
	
	
	private static final String SESSION_PREFIX="sessionId:";
	private EncryptHandle encrypt = new EncryptHandle();
	private static IFPRedisSessionManager ifpRedisSessionManager = new IFPRedisSessionManager();
	public static CacheManager ifpCacheManager = CacheHandler.getInstance();
	private Map<String,Map<String,Field>> fieldMap = new HashMap<String, Map<String,Field>>();
	private boolean inited = false;
	
	/**
	 * 维护中的会话
	 */
	private Map<String,Object> sessions = new ConcurrentHashMap<String,Object>();
	
	private String algorithm = "MD5";
	
	/**
	 * 会话超时时间，默认15分钟
	 */
	private int sessionTimeOut = 15 * 60;
	
	/**
	 * 检查会话超时的间隔时间，默认5分钟，如果不启动检查则配置bean时配置该参数为0
	 */
	private int sessionCheckInterval = 5 * 60 * 1000;
	
	
	private String sessionCache = "sessionCache";
	
	private Map<String, SessionElement> sessionElementMap = new HashMap<String,SessionElement>();
	

	/**
	 * 获取session的信息
	 */
	@Override
	public SessionInfo getSessionInfo(String sessionName, String sessionInfoName, String sessionId, Object handle) throws Exception {
		SessionNameElement sessionNameElement = getSessionNameElement(sessionName,sessionInfoName);
		SessionInfo sessionInfo = null;
		String cacheName = getCacheName(sessionName, sessionInfoName,sessionId);
		String key = sessionName + Constants.PERIOD + sessionInfoName + Constants.PERIOD + sessionId;
		sessionInfo = (UserSessionInfo)ifpCacheManager.get(cacheName,key, Constants.CACHE_VALUE_TYPE_OBJECT, sessionNameElement.getMaxInactiveInterval());
		if(sessionInfo != null && !checkTimeout(sessionInfo,sessionName,sessionInfoName)) {
			sessionInfo.setLastAccessTime(System.currentTimeMillis());//更新最后一次访问时间
			saveSessionInfo(sessionName, sessionInfoName, sessionInfo, handle);
			this.sessions.put(sessionName+Constants.PERIOD+sessionInfoName+Constants.PERIOD+sessionInfo.getSessionId(), sessionInfo);
			return sessionInfo;
		}
		return null;
	}
	
	/**
	 * 保存session的信息
	 */
	@Override
	public String saveSessionInfo(String sessionName,String sessionInfoName, SessionInfo sessionInfo,Object handle) throws Exception {
		String sessionId = sessionInfo.getSessionId();
		if(!StringUtils.hasText(sessionId)) {
			sessionId = generateSessionId(sessionName, null);
			sessionInfo.setSessionId(sessionId);
		}
		try {
			long currentTimeMillis = System.currentTimeMillis();
			if(sessionInfo.getCreateTime() == 0 ) {
				sessionInfo.setCreateTime(currentTimeMillis);
			}
			sessionInfo.setLastAccessTime(currentTimeMillis);
			String cacheName = getCacheName(sessionName, sessionInfoName , sessionInfo.getSessionId());
			ifpCacheManager.put(cacheName, sessionName+Constants.PERIOD+sessionInfoName+Constants.PERIOD+sessionInfo.getSessionId(), 
						sessionInfo,this.getMaxInactiveInterval(sessionName, sessionInfoName));
			this.sessions.put(sessionName+Constants.PERIOD+sessionInfoName+Constants.PERIOD+sessionInfo.getSessionId(), sessionInfo);
		} catch (Exception e) {
			throw new Exception(e);
		}
		return sessionId;
		
	}
	
	/**
	 * 生成唯一的SessionId。
	 * 
	 * @return session id
	 */
	private String generateSessionId(String sessionName,String keyValue) {

		try {
			String encryptionAlgorithm = getAlgorithm(sessionName);
			String sessionId = (String) SystemConf.getConfMap().get(Constants.SYS_IDC_FLAG)+
					Constants.UNDER_LINE+this.encrypt.getEncryptString(encryptionAlgorithm, keyValue);
			logger.info("generateSessionId :{}", sessionId);
			return sessionId;

		} catch (Exception ex) {

		}

		return null;
	}
	
	public String getAlgorithm(String sessionName) {
		if(this.sessionElementMap != null)
		{
			SessionElement sessionelement = this.sessionElementMap.get(sessionName);
			if(sessionelement.getAlgorithm() != null && !"".equals(sessionelement.getAlgorithm())) {
				return sessionelement.getAlgorithm();
			}
		}
		return algorithm;
	}
	
	/**
	 * 获取cachaName
	 * @param sessionName
	 * @param sessionInfoName
	 * @return
	 */
	public String getCacheName(String sessionName,String sessionInfoName,String sessionId)
	{
		if(sessionName != null && sessionInfoName != null)
		{
			SessionNameElement sessionNameElement = getSessionNameElement(sessionName, sessionInfoName);
			if(sessionNameElement == null) {
				return null;
			}
			String supportType = sessionNameElement.getCacheName();
			if(supportType != null && !"".equals(supportType))
			{
				String[] cacheNames = ifpCacheManager.getCacheNames();
				supportType=checkSysIdcFlag(supportType,sessionId);
				for(int i = 0 ; i < cacheNames.length; i ++)
				{
					if(supportType.equals(cacheNames[i])) {
						return cacheNames[i];
					}
				}
			}
		}else if(sessionName != null)
		{
			SessionElement sessionElement = this.sessionElementMap.get(sessionName);
			if(sessionElement == null) {
				return null;
			}
			String supportType  = sessionElement.getCacheName();
			if(supportType != null && !"".equals(supportType))
			{
				String[] cacheNames = ifpCacheManager.getCacheNames();
				supportType=checkSysIdcFlag(supportType,sessionId);
				for(int i = 0 ; i < cacheNames.length; i ++)
				{
					if(supportType.equals(cacheNames[i])) {
						return cacheNames[i];
					}
				}
			}
		}
		return sessionCache;
	}
	
	private String checkSysIdcFlag(String supportType , String sessionId) {
		if(sessionId!=null&&sessionId.contains(Constants.UNDER_LINE)){
			String[] sysIdcFlag = StrTokenizer.getCSVInstance(sessionId).setDelimiterChar('_').getTokenArray();
			supportType += Constants.UNDER_LINE+sysIdcFlag[0];
		}else{//若传进的sessionId为空或是之前缓存，则取本地缓存组标志
			supportType += Constants.UNDER_LINE+(String) SystemConf.getConfMap().get(Constants.SYS_IDC_FLAG);
		}
		return supportType;

	}
	
	/**
	 * 判断SessionInfo是否已经超时
	 * @param sessionInfo
	 * @param sessionName
	 * @return
	 */
	protected boolean checkTimeout(SessionInfo sessionInfo,String sessionName,String sessionInfoName){
		 if(this.getMaxInactiveInterval(sessionName,sessionInfoName) > 0)
		 {
			 long currentTime = System.currentTimeMillis();
			 boolean currentTimeCompareLastAccessTime = (currentTime - sessionInfo.getLastAccessTime()) > this.getMaxInactiveInterval(sessionName,sessionInfoName);
			 boolean currentTimeCompareCreateTime = false;
			 if(this.getforceExpirationPeriod(sessionName) > 0)
			 {
				 currentTimeCompareCreateTime = (currentTime - sessionInfo.getCreateTime()) > this.getforceExpirationPeriod(sessionName,sessionInfoName);
			 }
			 if(currentTimeCompareLastAccessTime||currentTimeCompareCreateTime)
			 {
				 removeSessionInfo(sessionInfo,sessionName,sessionInfoName);
			 }
			 return currentTimeCompareLastAccessTime||currentTimeCompareCreateTime;
		 }else{
			 return false;
		 }
	}
	
	/**
	 * 清除指定Session。
	 * 
	 * @param session 要清除的session
	 */
	public void removeSessionInfo(SessionInfo sessionInfo,String sessionName,String sessionInfoName) {
		if (sessionInfo == null){
			return;
		}
		
		removeSessionInfo(sessionName,sessionInfoName,sessionInfo.getSessionId());
	}
	
	public void removeSessionInfo(String sessionName,String sessionInfoName,String sessionId) {
		try {
			ifpCacheManager.remove(getCacheName(sessionName, sessionInfoName,sessionId), sessionName+Constants.PERIOD+sessionInfoName+Constants.PERIOD+sessionId);
		} catch (Exception e) {
			logger.error("delete session ERROR...", e);
		}
	}
	
	/**
	 * 默认为0，也就是永不失效（除非cookie失效）。例如，设置3600秒，表示用户离开浏览器1小时以后再回来，session将重新开始，老数据将被丢弃
	 * @param sessionName
	 * @return
	 */
	public int getMaxInactiveInterval(String sessionName,String sessionInfoName)
	{
		SessionNameElement sessionNameElement = getSessionNameElement(sessionName,sessionInfoName);
		if(sessionNameElement != null && sessionNameElement.getMaxInactiveInterval() >= 0 
				&& !"".equals(sessionNameElement.getMaxInactiveInterval()) 
				&& sessionNameElement.getMaxInactiveInterval() <= getMaxInactiveInterval(sessionName)
				&& getMaxInactiveInterval(sessionName) > 0 && sessionNameElement.getMaxInactiveInterval() > 0)
		{
			return sessionNameElement.getMaxInactiveInterval();
		}
		return getMaxInactiveInterval(sessionName);
	}
	
	/**
	 * 指定session强制作废期限，单位是秒。无论用户活动与否，从session创建之时算起，超过这个期限，session将被强制作废。万一cookie被盗，过了这个期限的话，那么无论如何，被盗的cookie就没有用了，默认是无限期；
	 * @param sessionName
	 * @return
	 */
	public int getforceExpirationPeriod(String sessionName)
	{
		if(this.sessionElementMap != null)
		{
			SessionElement sessionelement = this.sessionElementMap.get(sessionName);
			if(sessionelement.getForceExpirationPeriod() >= 0)
			{
				return sessionelement.getForceExpirationPeriod();
			}
		}
		return sessionTimeOut * 1000;
	}
	
	/**
	 * 指定session强制作废期限，单位是秒。无论用户活动与否，从session创建之时算起，超过这个期限，session将被强制作废。万一cookie被盗，过了这个期限的话，那么无论如何，被盗的cookie就没有用了，默认是无限期；
	 * @param sessionName
	 * @return
	 */
	public int getforceExpirationPeriod(String sessionName,String sessionInfoName)
	{
		SessionNameElement sessionNameElement = getSessionNameElement(sessionName,sessionInfoName);
		if(sessionNameElement != null && sessionNameElement.getForceExpirationPeriod() >= 0 
				&& !"".equals(sessionNameElement.getForceExpirationPeriod()) 
				&& sessionNameElement.getForceExpirationPeriod() <= getforceExpirationPeriod(sessionName)
				&& getforceExpirationPeriod(sessionName) > 0 && sessionNameElement.getForceExpirationPeriod() > 0)
		{
			return sessionNameElement.getForceExpirationPeriod();
		}
		return getforceExpirationPeriod(sessionName);
	}
	
	/**
	 * 默认为0，也就是永不失效（除非cookie失效）。例如，设置3600秒，表示用户离开浏览器1小时以后再回来，session将重新开始，老数据将被丢弃
	 * @param sessionName
	 * @return
	 */
	public int getMaxInactiveInterval(String sessionName)
	{
		if(this.sessionElementMap != null)
		{
			SessionElement sessionelement = this.sessionElementMap.get(sessionName);
			if(sessionelement.getMaxInactiveInterval() >= 0)
			{
				return sessionelement.getMaxInactiveInterval();
			}
		}
		return sessionTimeOut * 1000;
	}
	
	/**
	 * 初始化方法。
	 * 创建定时器和定时任务。
	 */
	@Override
	public void initialize() {
		
		/**
		 * 初始化加载sessioninfo映射表
		 */
		Map<String,SessionInfo> sessionInfoMap = new LinkedHashMap<>();
		sessionInfoMap.put(Constants.USER_SESSIONINFO, new UserSessionInfo());
		
		if(sessionInfoMap != null && !sessionInfoMap.isEmpty())
		{
			for (Map.Entry<String, SessionInfo> entity : sessionInfoMap.entrySet()) {
				Map<String,Field> map = new HashMap<String, Field>(0);
				Field[] fields = entity.getValue().getClass().getFields();
				for(Field field:fields)
				{
					map.put(field.getName(), field);
				}
				this.fieldMap.put(entity.getKey(), map);
			}
		}
		
		if(this.sessionCheckInterval > 0)
		{
			logger.info("Start up session checker for IFPSessionManager sessionTimeOut=" + this.sessionTimeOut + " check interval=" + this.sessionCheckInterval);
			ScheduledThreadPool scheduledThreadPool = new ScheduledThreadPool(
					Constants.IFP_SESSION_OUT_OF_TIME_CHECK_THREAD_POOL_NAME, this.sessionTimeOut, this.sessionCheckInterval, TimeUnit.MILLISECONDS);
			Runnable task = getClearSessionOfTimeoutTask();
		    scheduledThreadPool.execute(task);
			logger.info("Start up session checker for IFPSessionManager  ok!" );
		}
	}
	
	/**
	 * 获取定时任务
	 */
	public Runnable getClearSessionOfTimeoutTask() {
		Runnable task = new Runnable() {
	        @Override
	        public void run() {
	        	clearSessionOfTimeout(); 
	        }
	    };
	    return task;
	}
	
	/**
	 * 清除超时的session
	 */
	public void clearSessionOfTimeout() {
		logger.debug("Do the session check for IFPSessionManager");
		Object[] keys = null;
		try{
			keys = sessions.keySet().toArray();
		}catch(Exception e)
		{
			logger.error("Failed to do session time out check: ", e );
			return;
		}
		for( int i=0; i<keys.length; i++)
		{
			try {
				if(sessions.get( keys[i]) instanceof SessionInfo)
				{
					String key = (String)keys[i];
					SessionInfo sessionInfo = null;
					StringTokenizer keyName = new StringTokenizer(key, Constants.PERIOD);
					if(keyName.countTokens() == 3)
					{
						String sessionName = keyName.nextToken();
						String sessionInfoName = keyName.nextToken();
						String sessionId = keyName.nextToken();
						SessionNameElement sessionNameElement = getSessionNameElement(sessionName,sessionInfoName);
						String cacheName = getCacheName(sessionName, sessionInfoName,sessionId);
						sessionInfo = (SessionInfo) ifpCacheManager.get(cacheName, key, Constants.CACHE_VALUE_TYPE_OBJECT, sessionNameElement.getMaxInactiveInterval());
						
						if( sessionInfo == null ) {
							sessions.remove(key);
							continue;
						}
						
						if (checkTimeout(sessionInfo, sessionName, sessionInfoName)) {
							sessions.remove(key);
							logger.debug("Session time out: " + sessionInfo.getSessionId() );
						}
					}
				}
				
			} catch (Exception e) {
				logger.error("Failed to do session time out check", e );
			}
		}
	}
	
	/**
	 * 判断Session是否已经超时，可以通过扩展此方法，实现特定的Session检查机制
	 * @param session
	 * @return
	 */
	protected boolean checkTimeout(Session session, String sessionName) {
		if(session == null) {
			return true;
		}
		if (this.getMaxInactiveInterval(sessionName) > 0) {
			long currentTime = System.currentTimeMillis();
			boolean currentTimeCompareLastAccessTime = (currentTime - session.getLastAccessTime()) > this.getMaxInactiveInterval(sessionName);
			boolean currentTimeCompareCreateTime = false;
			if (this.getforceExpirationPeriod(sessionName) > 0) {
				currentTimeCompareCreateTime = (currentTime - session.getCreateTime()) > this.getforceExpirationPeriod(sessionName);
			}
			if (currentTimeCompareLastAccessTime || currentTimeCompareCreateTime) {
				removeSession(session);
			}
			return currentTimeCompareLastAccessTime || currentTimeCompareCreateTime;
		} else {
			return false;
		}
	}
	
	/**
	 * 清除指定Session。
	 * 
	 * @param session 要清除的session
	 */
	@Override
	public void removeSession(Session session) {
		if (session == null){
			return;
		}
		
		removeSession(session.getSessionName(),session.getId());
	}
	
	@Override
	public void removeSession(String sessionName,String sessionId) {
		try {
			ifpCacheManager.remove(getCacheName(sessionName,null,sessionId), SESSION_PREFIX + sessionId);
		} catch (Exception e) {
			logger.error("delete session ERROR...", e);
		}

	}
	
	/**
	 * 获取SessionNameElement配置信息
	 * @param sessionId
	 * @param sessionName
	 * @return
	 */
	public SessionNameElement getSessionNameElement(String sessionId,String sessionName)
	{
		SessionNameElement sessionNameElement = null;
		if(this.sessionElementMap != null)
		{
			SessionElement sessionelement = this.sessionElementMap.get(sessionId);
			if(sessionelement.getSessionNameMap() != null && sessionelement.getSessionNameMap().get(sessionName) != null) {
				return (SessionNameElement)sessionelement.getSessionNameMap().get(sessionName);
			}
		}
		return sessionNameElement;
	}

	public Map<String,SessionElement> getSessionElementMap() {
		return sessionElementMap;
	}
	
	@Override
	public void setSessionElementMap(Map<String, SessionElement> sessionElementMap) {
		this.sessionElementMap = sessionElementMap;
	}
	
	public static IFPRedisSessionManager getInstance() {
		ifpRedisSessionManager.initSession();
		return ifpRedisSessionManager;
	}

	/**
	 * 初始化session
	 */
	private synchronized void initSession() {
		if(inited) {
			return;
		}
		
		Map<String, SessionNameElement> sessionNameMap = new LinkedHashMap<>();
		SessionNameElement sessionNameElement = new SessionNameElement();
		sessionNameElement.setName(Constants.USER_SESSIONINFO);
		sessionNameElement.setMaxInactiveInterval(900);
		sessionNameElement.setCacheName(Constants.CACHENAME);
		sessionNameMap.put(Constants.USER_SESSIONINFO, sessionNameElement);

		Map<String, SessionElement> sessionElementMap = new LinkedHashMap<>();
		SessionElement sessionElement = new SessionElement();
		sessionElement.setId(Constants.SESSIONNAME);
		sessionElement.setMaxInactiveInterval(900);
		sessionElement.setKeepInTouch(true);
		sessionElement.setForceExpirationPeriod(0);
		sessionElement.setCacheName(Constants.CACHENAME);
		sessionElement.setSessionNameMap(sessionNameMap);
		sessionElementMap.put(Constants.SESSIONNAME, sessionElement);
		
		ifpRedisSessionManager.setSessionElementMap(sessionElementMap);
		
		initialize();
		
		inited = true;
	}
	
}
