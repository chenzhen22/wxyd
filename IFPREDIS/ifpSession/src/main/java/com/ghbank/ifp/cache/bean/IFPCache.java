package com.ghbank.ifp.cache.bean;

public class IFPCache extends AbstractCache {
	public CacheServices servers = new CacheServices();

	public CacheServices getServers() {
		return this.servers;
	}

	public void setServers(CacheServices servers) {
		this.servers = servers;
	}
}
