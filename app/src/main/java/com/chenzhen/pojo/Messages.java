package com.chenzhen.pojo;

import lombok.Data;

@Data
public class Messages {

	private String ip;
	private String info;
	private String operTime;

	public Messages(String ip, String info, String operTime) {
		this.ip = ip;
		this.info = info;
		this.operTime = operTime;
	}

	public Messages(String ip, String info) {
		this.ip = ip;
		this.info = info;
	}
	
}
