package com.ghbank.ifp.cache.bean;

public class CacheServer {
	public String masterHost;
	public String masterPost;
	public String masterPassword;
	public String bakHost;
	public String bakPort;
	public String bakPassword;
	public String sentinelHost;
	public String sentinelPost;
	public String sentinelPassword;
	public String masterName;
	public String weight;
	public String timeout;
	public String databases;

	public String getMasterHost() {
		return this.masterHost;
	}

	public void setMasterHost(String masterHost) {
		this.masterHost = masterHost;
	}

	public int getMasterPost() {
		return Integer.parseInt(this.masterPost);
	}

	public void setMasterPost(String masterPost) {
		this.masterPost = masterPost;
	}

	public String getMasterPassword() {
		return this.masterPassword;
	}

	public void setMasterPassword(String masterPassword) {
		this.masterPassword = masterPassword;
	}

	public String getBakHost() {
		return this.bakHost;
	}

	public void setBakHost(String bakHost) {
		this.bakHost = bakHost;
	}

	public int getBakPort() {
		return Integer.parseInt(this.bakPort);
	}

	public void setBakPort(String bakPort) {
		this.bakPort = bakPort;
	}

	public String getBakPassword() {
		return this.bakPassword;
	}

	public void setBakPassword(String bakPassword) {
		this.bakPassword = bakPassword;
	}

	public int getWeight() {
		return Integer.parseInt(this.weight);
	}

	public void setWeight(String weight) {
		this.weight = weight;
	}

	public int getTimeout() {
		return Integer.parseInt(this.timeout);
	}

	public void setTimeout(String timeout) {
		this.timeout = timeout;
	}

	public int getDatabases() {
		return Integer.parseInt(this.databases);
	}

	public void setDatabases(String databases) {
		this.databases = databases;
	}

	public String getSentinelHost() {
		return this.sentinelHost;
	}

	public void setSentinelHost(String sentinelHost) {
		this.sentinelHost = sentinelHost;
	}

	public int getSentinelPost() {
		return Integer.parseInt(this.sentinelPost);
	}

	public void setSentinelPost(String sentinelPost) {
		this.sentinelPost = sentinelPost;
	}

	public String getSentinelPassword() {
		return this.sentinelPassword;
	}

	public void setSentinelPassword(String sentinelPassword) {
		this.sentinelPassword = sentinelPassword;
	}

	public String getMasterName() {
		return this.masterName;
	}

	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}
}
