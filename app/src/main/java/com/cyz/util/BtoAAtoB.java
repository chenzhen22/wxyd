package com.cyz.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BtoAAtoB {
	private static String base64hash = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	
	public static boolean isMatcher(String inStr, String reg) {
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(inStr);
		if (matcher.matches()) {
			return true;
		}
		return false;
	}

	public static String btoa(String inStr) {
		if (inStr == null || isMatcher(inStr, "([^\\u0000-\\u00ff])")) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		int i = 0;
		int mod = 0;
		int ascii;
		int prev = 0;
		while (i < inStr.length()) {
			ascii = inStr.charAt(i);
			mod = i % 3;
			switch (mod) {
			case 0:
				result.append(String.valueOf(base64hash.charAt(ascii >> 2)));
				break;
			case 1:
				result.append(String.valueOf(base64hash.charAt((prev & 3) << 4 | (ascii >> 4))));
				break;
			case 2:
				result.append(String.valueOf(base64hash.charAt((prev & 0x0f) << 2 | (ascii >> 6))));
				result.append(String.valueOf(base64hash.charAt(ascii & 0x3f)));
				break;
			}
			prev = ascii;
			i++;
		}
		if (mod == 0) {
			result.append(String.valueOf(base64hash.charAt((prev & 3) << 4)));
			result.append("==");
		} else if (mod == 1) {
			result.append(String.valueOf(base64hash.charAt((prev & 0x0f) << 2)));
			result.append("=");
		}
		return result.toString();
	}
	
	public static String atob(String inStr) {
		if(inStr == null) {
			return null;
		}
		inStr = inStr.replaceAll("\\s|=", "");
		StringBuilder result = new StringBuilder();
		int cur;
		int prev = -1;
		int mod;
		int i = 0;
		while (i < inStr.length()) {
			cur = base64hash.indexOf(inStr.charAt(i));
			mod = i%4;
			switch (mod) {
			case 0:
				break;
			case 1:
				result.append(String.valueOf((char)(prev << 2 | cur >> 4)));
				break;
			case 2:
				result.append(String.valueOf((char)((prev & 0x0f) << 4 | cur >> 2)));
				break;
			case 3:
				result.append(String.valueOf((char)((prev & 3) << 6 | cur)));
				break;
			}
			prev = cur;
			i++;
		}
		return result.toString();
	}

	public static void main(String[] args) throws UnsupportedEncodingException {
		String transData = "JTdCJTIyY21pc2xvYW5ObyUyMiUzQSUyMjIwMzAwODA1MDkyMDM3NjU3JTIyJTJDJTIyYWNjdE5hbWUlMjIlM0ElMjIlRTklODclOEQlRTUlQkElODYlRTQlQkMlOUYlRTQlQkYlQTElRTQlQkIlODElRTUlODclQkIlRTYlOUYlQjQlRTYlQjIlQjklRTYlOUMlQkElRTglODIlQTElRTQlQkIlQkQlRTYlOUMlODklRTklOTklOTAlMjIlMkMlMjJzZXR0bGVCYXNlQWNjdE5vJTIyJTNBJTIyODA2ODgwMTAwMDE3MjM2JTIyJTJDJTIyc2V0dGxlV3RyQmFzZUFjY3RObyUyMiUzQSUyMjgwNjg4MDEwMDAxNzIxOCUyMiUyQyUyMnNldHRsZVd0ckFjY3ROYW1lJTIyJTNBJTIyJUU2JTk3JUEwJUU5JTk0JUExJUU1JThEJThFJUU2JUI1JUI3JUU2JUE4JUFBJUU0JUJBJTk1JUU5JTgwJUEwJUU3JUJBJUI4JUU4JTgyJUExJUU0JUJCJUJEJUU2JTlDJTg5JTIyJTJDJTIyZGRBbXQlMjIlM0ElMjIxMSUyMiUyQyUyMmFjY3RPcGVuRGF0ZSUyMiUzQSUyMjIwMzAwODA1JTIyJTJDJTIybWF0dXJpdHlEYXRlJTIyJTNBJTIyMjAzMDEyMDElMjIlMkMlMjJhY2N0U3RhdHVzJTIyJTNBJTIyQSUyMiUyQyUyMnJlYWxSYXRlJTIyJTNBJTIyMC4wMyUyMiUyQyUyMnNjaGVkTW9kZSUyMiUzQSUyMjQlMjIlMkMlMjJ0b3RhbEludEFtdCUyMiUzQSUyMjAlMjIlMkMlMjJwYWlkSW50QW10JTIyJTNBJTIyMCUyMiUyQyUyMmV4dGVuZFRpbWVzJTIyJTNBJTIyMCUyMiUyQyUyMmJhbGFuY2UlMjIlM0ElMjIxMSUyMiUyQyUyMnBhc3REdWVSYXRlJTIyJTNBJTIyMC4wNCUyMiUyQyUyMmN5Y2xlRnJlcSUyMiUzQSUyMk0xJTIyJTJDJTIyc2NoZWRJbnRBbXQlMjIlM0ElMjIwJTIyJTJDJTIycmVtSW50QW10JTIyJTNBJTIyMCUyMiUyQyUyMmlzRXhGbGFnJTIyJTNBJTIyJTIyJTJDJTIyY2hhcmdlcmF0ZSUyMiUzQSUyMjIwJTIyJTJDJTIyY2hhcmdlQW1vdXQlMjIlM0ElMjIyLjIwJTIyJTdE";
		transData = BtoAAtoB.atob(transData);
		transData = URLDecoder.decode(transData, "UTF-8");
		System.out.println(transData);
	}
}
