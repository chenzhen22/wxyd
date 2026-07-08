package com.chenzhen.pojo;

import lombok.Data;

@Data
public class OperInfo {

	private String ip;
	private String info;
	private String operTime;

	public OperInfo(String ip, String info, String operTime) {
		this.ip = ip;
		this.info = info;
		this.operTime = operTime;
	}

	
}
