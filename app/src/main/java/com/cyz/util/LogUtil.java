package com.cyz.util;

public class LogUtil {

    /**
     * 获取日志线程
     * @return
     */
    public static String getTraceId() {
        final StringBuffer sb = new StringBuffer();
        sb.append(System.currentTimeMillis()).append(CommUtils.generateRandomString(7));
        return sb.toString();
    }

}
