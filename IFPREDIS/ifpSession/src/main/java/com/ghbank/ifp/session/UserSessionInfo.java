package com.ghbank.ifp.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话缓存信息
 * 
 */
public class UserSessionInfo extends SessionInfo {
	
	private static final long serialVersionUID = 1L;
	// 是否已经被不同的设备挤掉
	private boolean hasBeenCancelled = false;
	// 个人客户号
	private String personalCifNo;
	// 手机号
	private String mobile;
	// 安全手机号
	private String safeMobile;
	// 证件号
	private String certNo;
	// 设备号
	private String devicesNo;
	// 登录状态
	private String loginStatus;
	// 客户Ip地址
	private String clientIp;
	// 客户mac地址
	private String clientMac;

	// 个人手机银行会话数据存储MAP
	private Map<String, Object> sessionInfoNMBMap = new ConcurrentHashMap<>();
	// 企业手机银行会话数据存储MAP
	private Map<String, Object> sessionInfoTBMMap = new ConcurrentHashMap<>();
	// 微银行小程序会话数据存储MAP
	private Map<String, Object> sessionInfoMPPMap = new ConcurrentHashMap<>();
	// 零售信贷小程序会话数据存储MAP
	private Map<String, Object> sessionInfoXFAMap = new ConcurrentHashMap<>();
	// 个人贷款前置数据存储MAP
	private Map<String, Object> sessionInfoNLSMap = new ConcurrentHashMap<>();
	
	public boolean getHasBeenCancelled() {
		return hasBeenCancelled;
	}

	public void setHasBeenCancelled(boolean hasBeenCancelled) {
		this.hasBeenCancelled = hasBeenCancelled;
	}

	public String getPersonalCifNo() {
		return personalCifNo;
	}

	public void setPersonalCifNo(String personalCifNo) {
		this.personalCifNo = personalCifNo;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getSafeMobile() {
		return safeMobile;
	}

	public void setSafeMobile(String safeMobile) {
		this.safeMobile = safeMobile;
	}

	public String getCertNo() {
		return certNo;
	}

	public void setCertNo(String certNo) {
		this.certNo = certNo;
	}

	public String getDevicesNo() {
		return devicesNo;
	}

	public void setDevicesNo(String devicesNo) {
		this.devicesNo = devicesNo;
	}

	public String getLoginStatus() {
		return loginStatus;
	}

	public void setLoginStatus(String loginStatus) {
		this.loginStatus = loginStatus;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getClientMac() {
		return clientMac;
	}

	public void setClientMac(String clientMac) {
		this.clientMac = clientMac;
	}
	
	public Map<String, Object> getSessionInfoNMBMap() {
		return sessionInfoNMBMap;
	}
	public void setSessionInfoNMBMap(Map<String, Object> sessionInfoNMBMap) {
		this.sessionInfoNMBMap = sessionInfoNMBMap;
	}
	public Map<String, Object> getSessionInfoTBMMap() {
		return sessionInfoTBMMap;
	}
	public void setSessionInfoTBMMap(Map<String, Object> sessionInfoTBMMap) {
		this.sessionInfoTBMMap = sessionInfoTBMMap;
	}
	public Map<String, Object> getSessionInfoMPPMap() {
		return sessionInfoMPPMap;
	}
	public void setSessionInfoMPPMap(Map<String, Object> sessionInfoMPPMap) {
		this.sessionInfoMPPMap = sessionInfoMPPMap;
	}
	public Map<String, Object> getSessionInfoXFAMap() {
		return sessionInfoXFAMap;
	}
	public void setSessionInfoXFAMap(Map<String, Object> sessionInfoXFAMap) {
		this.sessionInfoXFAMap = sessionInfoXFAMap;
	}
	public Map<String, Object> getSessionInfoNLSMap() {
		return sessionInfoNLSMap;
	}
	public void setSessionInfoNLSMap(Map<String, Object> sessionInfoNLSMap) {
		this.sessionInfoNLSMap = sessionInfoNLSMap;
	}
	
	public UserSessionInfo clearChnMap(UserSessionInfo userSessionInfo, String chn) {
		Map<String, Object> nullMap  = new ConcurrentHashMap<>();
		switch (chn) {
		case "NMB":
			userSessionInfo.setSessionInfoTBMMap(nullMap);
			userSessionInfo.setSessionInfoMPPMap(nullMap);
			userSessionInfo.setSessionInfoXFAMap(nullMap);
			userSessionInfo.setSessionInfoNLSMap(nullMap);
			break;
		case "TBM":
			userSessionInfo.setSessionInfoNMBMap(nullMap);
			userSessionInfo.setSessionInfoMPPMap(nullMap);
			userSessionInfo.setSessionInfoXFAMap(nullMap);
			userSessionInfo.setSessionInfoNLSMap(nullMap);
			break;
		case "MPP":
			userSessionInfo.setSessionInfoNMBMap(nullMap);
			userSessionInfo.setSessionInfoTBMMap(nullMap);
			userSessionInfo.setSessionInfoXFAMap(nullMap);
			userSessionInfo.setSessionInfoNLSMap(nullMap);
			break;
		case "XFA":
			userSessionInfo.setSessionInfoNMBMap(nullMap);
			userSessionInfo.setSessionInfoTBMMap(nullMap);
			userSessionInfo.setSessionInfoMPPMap(nullMap);
			userSessionInfo.setSessionInfoNLSMap(nullMap);
			break;
		case "NLS":
			userSessionInfo.setSessionInfoNMBMap(nullMap);
			userSessionInfo.setSessionInfoTBMMap(nullMap);
			userSessionInfo.setSessionInfoMPPMap(nullMap);
			userSessionInfo.setSessionInfoXFAMap(nullMap);
			break;
		case "ALL":
			break;
		default:
			userSessionInfo.setSessionInfoNMBMap(null);
			userSessionInfo.setSessionInfoTBMMap(null);
			userSessionInfo.setSessionInfoMPPMap(null);
			userSessionInfo.setSessionInfoXFAMap(null);
			userSessionInfo.setSessionInfoNLSMap(null);
		}
		return userSessionInfo;
	}

	@Override
	public String toString() {
		return "UserSessionInfo [sessionId=" + sessionId + ", createTime=" + createTime + ", lastAccessTime="
				+ lastAccessTime + ", hasBeenCancelled=" + hasBeenCancelled + ", personalCifNo=" + personalCifNo
				+ ", mobile=" + mobile + ", safeMobile=" + safeMobile + ", certNo=" + certNo + ", devicesNo="
				+ devicesNo + ", loginStatus=" + loginStatus + ", clientIp=" + clientIp + ", clientMac=" + clientMac
				+ ", sessionInfoNMBMap=" + sessionInfoNMBMap + ", sessionInfoTBMMap=" + sessionInfoTBMMap
				+ ", sessionInfoMPPMap=" + sessionInfoMPPMap + ", sessionInfoXFAMap=" + sessionInfoXFAMap
				+ ", sessionInfoNLSMap=" + sessionInfoNLSMap + "]";
	}
}
