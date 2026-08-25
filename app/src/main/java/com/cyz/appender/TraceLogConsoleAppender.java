package com.cyz.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;


public class TraceLogConsoleAppender extends AppenderBase<ILoggingEvent> {

    public TraceLogConsoleAppender() {
    }

    @Override
    protected void append(ILoggingEvent event) {
        //获取日志信息，可通过ELK进行日志抽取，也可记录到数据库进行分析等
        //打印信息所在类的完整路径
        String loggerName = event.getLoggerName();
        System.out.println("loggerName:" + loggerName);
        //日志信息
        String message = event.getFormattedMessage();
        System.out.println("message:" + message);
        //日志级别
        String level = event.getLevel().toString();
        System.out.println("level:" + level);

    }
}
