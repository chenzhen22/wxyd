package com.ghbank.ifp.session;

import java.util.Enumeration;

public interface Session {
	void setAttribute(String var1, Object var2);

	Object getAttribute(String var1);

	void removeAttribute(String var1);

	Enumeration getAttributeNames();

	String getId();

	String getSessionName();

	long getLastAccessTime();

	void setLastAccessTime(long var1);

	long getCreateTime();
}