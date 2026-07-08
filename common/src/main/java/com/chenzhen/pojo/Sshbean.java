package com.chenzhen.pojo;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class Sshbean {
	
	private String host;
	private int port;
	private String username;
	private String password;
	
	public Sshbean(String host, int port, String username, String password) {
		this.host = host;
		this.port = StringUtils.isEmpty(port+"")?22:port;
		this.username = StringUtils.isEmpty(username)?"showlog":username;
		this.password = StringUtils.isEmpty(username)?"PassWord123?":password;
	}
	
}
