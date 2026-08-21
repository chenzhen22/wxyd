package com.chenzhen.pojo;

import lombok.Data;

@Data
public class UdInfo {
	private String no;
	private String statue;

	public static UdInfo getInstance() {
		return new UdInfo();
	}

	public UdInfo no(String no) {
		this.no = no;
		return this;
	}

	public UdInfo statue(String statue) {
		this.statue = statue;
		return this;
	}

}
