package com.ghbank.ifp.session;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghbank.ifp.base.Constants;
import com.ghbank.ifp.cache.handle.CacheHandler;
import com.ghbank.ifp.redis.IFPRedisSessionManager;
import com.ghbank.ifp.redis.config.SystemConf;
import com.ghbank.ifp.util.StringUtils;

/**
 * IFP会话管理类
 *
 */
public class IFPSessionManager {

	private static Logger logger = LoggerFactory.getLogger(IFPSessionManager.class);
	
	private static SessionManager sessionManager;
	private static IFPSessionManager ifpSessionManager = new IFPSessionManager();
		
	/**
	 * 获取session信息
	 * 
	 * @param sessionID 会话ID
	 * @param chn 渠道
	 * @return
	 * @throws Exception 
	 */
	public UserSessionInfo getSessionInfo (String sessionId, String chn) throws Exception {
		checkChn(chn);
		logger.info("IFPSessionManager getSessionInfo sessionId : {}, chn : {}", sessionId, chn);
		UserSessionInfo userSessionInfo = (UserSessionInfo) sessionManager.getSessionInfo(Constants.SESSIONNAME, Constants.USER_SESSIONINFO, sessionId, null);
		if(null != userSessionInfo) {
			userSessionInfo = userSessionInfo.clearChnMap(userSessionInfo, chn);
		}
		return userSessionInfo;
	}
	
	/**
	 * 保存session信息
	 * 
	 * @param sessionID 会话ID
	 * @param userSesssionInfo 会话缓存信息
	 * @param chn 渠道
	 * @throws Exception 
	 */
	public String saveSessionInfo (UserSessionInfo userSessionInfo, String chn) throws Exception {
		checkChn(chn);
		logger.info("IFPSessionManager saveSessionInfo userSessionInfo : {}, chn : {}", userSessionInfo, chn);
		String sessionId = null;
		UserSessionInfo session = (UserSessionInfo) sessionManager.getSessionInfo(Constants.SESSIONNAME, Constants.USER_SESSIONINFO, userSessionInfo.getSessionId(), null);
		if(null != session && StringUtils.hasText(session.getSessionId())) { //更新
			switch (chn) {
			case "NMB":
				session.setSessionInfoNMBMap(userSessionInfo.getSessionInfoNMBMap());
				break;
			case "TBM":
				session.setSessionInfoTBMMap(userSessionInfo.getSessionInfoTBMMap());
				break;
			case "MPP":
				session.setSessionInfoMPPMap(userSessionInfo.getSessionInfoMPPMap());
				break;
			case "XFA":
				session.setSessionInfoXFAMap(userSessionInfo.getSessionInfoXFAMap());
				break;
			case "NLS":
				session.setSessionInfoNLSMap(userSessionInfo.getSessionInfoNLSMap());
				break;
			default:
				break;
			}
			sessionId = sessionManager.saveSessionInfo(Constants.SESSIONNAME, Constants.USER_SESSIONINFO, session, null);
		} else { //新增
			sessionId = sessionManager.saveSessionInfo(Constants.SESSIONNAME, Constants.USER_SESSIONINFO, userSessionInfo, null);
		}
		return sessionId;
		
	}
	
	/**
	 * 清理session信息
	 * 
	 * @param sessionID 会话ID
	 * @param userSesssionInfo 会话缓存信息
	 * @param chn 渠道
	 * @throws Exception 
	 */
	/*public void clearSessionInfo (String sessionID, UserSessionInfo userSesssionInfo, String chn) {
		
	}*/
	
	/**
	 * session保活
	 * @param sessionId
	 * @throws Exception 
	 */
	public void keepSessionLive(String sessionId) throws Exception {
		UserSessionInfo userSessionInfo = (UserSessionInfo) sessionManager.getSessionInfo(Constants.SESSIONNAME, Constants.USER_SESSIONINFO, sessionId, null);
		if(null != userSessionInfo && StringUtils.hasText(userSessionInfo.getSessionId())) {
			sessionManager.saveSessionInfo(Constants.SESSIONNAME, Constants.USER_SESSIONINFO, userSessionInfo, null);
		}
	}
	
	/**
	 * 选择机房标识，默认使用redis.properties:sys.idc,这个方法可以手动切换机房
	 * @param sys_idc
	 */
	public void chooseSysIdc(String sys_idc) {
		if("X".equals(sys_idc) || "Y".equals(sys_idc)) {
			logger.info("IFPSessionManager chooseSysIdc sys_idc : {}", sys_idc);
			SystemConf.setConfMap(Constants.SYS_IDC_FLAG, sys_idc);
		}
	}
	
	/**
	 * 校验渠道
	 * @param chn
	 * @throws Exception
	 */
	private static void checkChn(String chn) throws Exception {
		if(!Constants.CHN.contains(chn)) {
			throw new Exception("渠道未准入");
		}
	}
	
	/**
	 * IFPSessionManager实例化
	 * @return
	 */
	public static IFPSessionManager getInstance(InputStream is) {
		sessionManager = IFPRedisSessionManager.getInstance();
		CacheHandler.getInstance().init(is);
		return ifpSessionManager;
	}
	
}
