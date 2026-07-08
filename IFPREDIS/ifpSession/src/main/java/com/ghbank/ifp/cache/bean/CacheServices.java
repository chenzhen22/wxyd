package com.ghbank.ifp.cache.bean;

import java.util.ArrayList;
import java.util.List;

public class CacheServices {
	public List<CacheServer> servers = new ArrayList();

	public List<CacheServer> getServers() {
		return this.servers;
	}

	public void setServers(List<CacheServer> servers) {
		this.servers = servers;
	}
}
