package com.cyz.dubbo.util;

import java.security.SecureRandom;

/**
 * 简易字符串工具（移植自 do_dubbo）。
 */
public final class StringUtil {

    private static final char[] ALPHANUM =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private StringUtil() {
    }

    /**
     * 生成指定长度的随机字母数字字符串（大小写字母 + 数字）。
     *
     * @param length 期望长度；若 &lt;=0 返回空串
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            return "";
        }
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = ALPHANUM[RANDOM.nextInt(ALPHANUM.length)];
        }
        return new String(buf);
    }
}
