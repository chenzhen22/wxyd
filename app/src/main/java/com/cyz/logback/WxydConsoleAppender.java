package com.chenzhen.logback;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.chenzhen.util.CommUtils;

import org.springframework.util.StringUtils;

/**
 * Console appender that only emits on the designated "local" server (the host
 * whose IP matches the {@code localIp} config key). Formerly a deployment gate
 * keyed off a Nacos value; now keyed off {@code common.properties}.
 * <p>
 * When {@code localIp} is unconfigured (blank), the gate is disabled and the
 * appender always emits — so local dev boots with visible console output.
 */
public class WxydConsoleAppender<E> extends ConsoleAppender<E> {


    @Override
    protected void append(E eventObject) {
        String hostIp = CommUtils.getHostAddress();
        String localIp = CommUtils.getParamValue("localIp");
        if (StringUtils.hasText(localIp) && !localIp.equals(hostIp)) {
            return;
        }
        subAppend(eventObject);
    }

}
