package com.ghbank.ifp.redis.config;

public class HostConfig {
	private String host;
	private int port;
	private String password;
	private int timeout;
	private int databases;
	private String masterName;

	public HostConfig() {
	}

	public HostConfig(String host, int port, String password, int timeout, int databases) {
		this.host = host;
		this.port = port;
		this.password = password;
		this.timeout = timeout;
		this.databases = databases;
	}

	public HostConfig(String host, int port, String password, int timeout, int databases, String masterName) {
		this.host = host;
		this.port = port;
		this.password = password;
		this.timeout = timeout;
		this.databases = databases;
		this.masterName = masterName;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getTimeout() {
		return this.timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String toString() {
		return this.host + ":" + this.port;
	}

	public int getDatabases() {
		return this.databases;
	}

	public void setDatabases(int databases) {
		this.databases = databases;
	}

	public String getMasterName() {
		return this.masterName;
	}

	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}
}
