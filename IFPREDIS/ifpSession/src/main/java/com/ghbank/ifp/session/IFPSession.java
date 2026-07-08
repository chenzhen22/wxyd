package com.ghbank.ifp.session;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

public class IFPSession implements Session, Serializable {
	private String sessionId;
	private String sessionName;
	private long lastAccessTime;
	private Hashtable attributes;
	private long createTime;
	private long maxInactiveInterval;
	private boolean keepInTouch;
	private long forceExpirationPeriod;

	public IFPSession() {
	}

	public IFPSession(String sessionId) {
		this.attributes = new Hashtable();
		this.sessionId = sessionId;
		this.createTime = System.currentTimeMillis();
		this.lastAccessTime = this.createTime;
	}

	public IFPSession(String sessionId, String sessionName) {
		this.attributes = new Hashtable();
		this.sessionId = sessionId;
		this.createTime = System.currentTimeMillis();
		this.lastAccessTime = this.createTime;
		this.sessionName = sessionName;
	}

	public void setAttribute(String key, Object value) {
		this.attributes.put(key, value);
	}

	public Object getAttribute(String key) {
		return this.attributes.get(key);
	}

	public void removeAttribute(String key) {
		this.attributes.remove(key);
	}

	public Enumeration getAttributeNames() {
		this.attributes.keySet();
		return this.attributes.keys();
	}

	public String getId() {
		return this.sessionId;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public long getLastAccessTime() {
		return this.lastAccessTime;
	}

	public void setLastAccessTime(long lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}

	public Hashtable getAttributes() {
		return this.attributes;
	}

	public void setAttributes(Hashtable attributes) {
		this.attributes = attributes;
	}

	public long getCreateTime() {
		return this.createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	public void setMaxInactiveInterval(long maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public boolean isKeepInTouch() {
		return this.keepInTouch;
	}

	public void setKeepInTouch(boolean keepInTouch) {
		this.keepInTouch = keepInTouch;
	}

	public long getForceExpirationPeriod() {
		return this.forceExpirationPeriod;
	}

	public void setForceExpirationPeriod(long forceExpirationPeriod) {
		this.forceExpirationPeriod = forceExpirationPeriod;
	}

	public String getSessionName() {
		return this.sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}
}
