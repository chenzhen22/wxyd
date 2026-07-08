package com.chenzhen.logback;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.chenzhen.util.CommUtils;

public class WxydConsoleAppender<E> extends ConsoleAppender<E> {


    @Override
    protected void append(E eventObject) {
        String hostIp = CommUtils.getHostAddress();
        String localIp = CommUtils.getParamValue("localIp");
        if (!localIp.equals(hostIp)) {
            return;
        }
        subAppend(eventObject);
    }

}
