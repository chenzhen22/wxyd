package com.ghbank.ifp.session;

import java.util.Map;

import com.ghbank.ifp.data.SessionElement;

public interface SessionManager {
	
	SessionInfo getSessionInfo(String sessionName, String sessionInfo, String sessionId, Object handle) throws Exception ;
	
	void setSessionElementMap(Map<String, SessionElement> sessionElementMap);

	String saveSessionInfo(String sessionName, String sessionInfoName, SessionInfo sessionInfo, Object handle) throws Exception;

	void initialize();

	void removeSession(Session session);

	void removeSession(String sessionName, String sessionId);

}
