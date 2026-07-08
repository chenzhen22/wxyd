package com.ghbank.ifp.cache.bean;

public interface ICache {
	String getId();

	void setId(String var1);

	String getType();

	void setType(String var1);

	long getIntervalTime();

	void setIntervalTime(String var1);

	int getMaxIdleConn();

	void setMaxIdleConn(String var1);

	boolean isOpen();

	void setOpen(String var1);

	int getMinIdleConn();

	void setMinIdleConn(String var1);

	int getMaxConn();

	void setMaxConn(String var1);

	long getMaxWait();

	void setMaxWait(String var1);
}
