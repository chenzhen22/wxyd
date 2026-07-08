package com.ghbank.ifp.cache.bean;

import com.ghbank.ifp.util.StringUtils;

public abstract class AbstractCache implements ICache {
	public String id;
	public String type;
	public String intervalTime;
	public String maxIdleConn;
	public String minIdleConn;
	public String maxConn;
	public String maxWait;
	public String open;

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getIntervalTime() {
		return StringUtils.hasText(this.intervalTime) ? Long.parseLong(this.intervalTime) : 0L;
	}

	public void setIntervalTime(String intervalTime) {
		this.intervalTime = intervalTime;
	}

	public int getMaxIdleConn() {
		return StringUtils.hasText(this.maxIdleConn) ? Integer.parseInt(this.maxIdleConn) : 50;
	}

	public void setMaxIdleConn(String maxIdleConn) {
		this.maxIdleConn = maxIdleConn;
	}

	public int getMinIdleConn() {
		return StringUtils.hasText(this.minIdleConn) ? Integer.parseInt(this.minIdleConn) : 5;
	}

	public void setMinIdleConn(String minIdleConn) {
		this.minIdleConn = minIdleConn;
	}

	public int getMaxConn() {
		return StringUtils.hasText(this.maxConn) ? Integer.parseInt(this.maxConn) : 200;
	}

	public long getMaxWait() {
		return StringUtils.hasText(this.maxWait) ? (long) Integer.parseInt(this.maxWait) : 10000L;
	}

	public void setMaxWait(String maxWait) {
		this.maxWait = maxWait;
	}

	public void setMaxConn(String maxConn) {
		this.maxConn = maxConn;
	}

	public boolean isOpen() {
		return StringUtils.hasText(this.open) ? Boolean.parseBoolean(this.open) : true;
	}

	public void setOpen(String open) {
		this.open = open;
	}
}