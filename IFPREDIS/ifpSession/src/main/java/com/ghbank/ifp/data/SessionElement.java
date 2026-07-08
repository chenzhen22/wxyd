package com.ghbank.ifp.data;

import java.util.Map;

public class SessionElement {
	private String id;
	private int maxInactiveInterval = -1;
	private boolean keepInTouch = true;
	private int forceExpirationPeriod = -1;
	private String dataSourceName;
	private String cacheName;
	private String algorithm;
	private boolean saveCookie;
	private boolean httpOnly = false;
	private boolean cookieSecure = false;
	private String clazz;
	private Map<String, SessionNameElement> sessionNameMap;

	public SessionElement() {
	}

	public SessionElement(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isKeepInTouch() {
		return this.keepInTouch;
	}

	public void setKeepInTouch(boolean keepInTouch) {
		this.keepInTouch = keepInTouch;
	}

	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval * 1000;
	}

	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public int getForceExpirationPeriod() {
		return this.forceExpirationPeriod * 1000;
	}

	public void setForceExpirationPeriod(int forceExpirationPeriod) {
		this.forceExpirationPeriod = forceExpirationPeriod;
	}

	public String getDataSourceName() {
		return this.dataSourceName;
	}

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	public String getCacheName() {
		return this.cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public String getAlgorithm() {
		return this.algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public boolean isSaveCookie() {
		return this.saveCookie;
	}

	public void setSaveCookie(boolean saveCookie) {
		this.saveCookie = saveCookie;
	}

	public Map<String, SessionNameElement> getSessionNameMap() {
		return this.sessionNameMap;
	}

	public void setSessionNameMap(Map<String, SessionNameElement> sessionNameMap) {
		this.sessionNameMap = sessionNameMap;
	}

	public String toString() {
		return ",id:" + this.id + "}";
	}

	public boolean isHttpOnly() {
		return this.httpOnly;
	}

	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	public String getClazz() {
		return this.clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public boolean isCookieSecure() {
		return this.cookieSecure;
	}

	public void setCookieSecure(boolean cookieSecure) {
		this.cookieSecure = cookieSecure;
	}
}
