package com.ghbank.ifp.util;

public class StringUtils {
	
	public static boolean hasText(CharSequence str) {
		return (str != null && str.length() > 0 && containsText(str));
	}
	
	public static boolean hasText(String str) {
		return (str != null && !str.trim().isEmpty() && containsText(str));
	}
	
	private static boolean containsText(CharSequence str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

}
