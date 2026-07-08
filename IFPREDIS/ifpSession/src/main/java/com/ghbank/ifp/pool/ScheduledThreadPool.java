package com.ghbank.ifp.pool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import org.apache.commons.lang3.concurrent.BasicThreadFactory.Builder;

public class ScheduledThreadPool extends AbstractThreadPool {
	private ScheduledExecutorService executorService;
	private long initialDelay;
	private long period;
	private TimeUnit timeUnit;

	@Deprecated
	public ScheduledThreadPool(int num) {
		ThreadFactory namedThreadFactory = (new ThreadFactoryBuilder()).setNameFormat("scheduled-thread-pool-%d")
				.build();
		ExecutorService pool = new ThreadPoolExecutor(num, num, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue(1024), namedThreadFactory, new AbortPolicy());
		this.setThreadPool(pool);
	}

	public ScheduledThreadPool(String poolName, long initialDelay, long period, TimeUnit timeUnit) {
		this.executorService = new ScheduledThreadPoolExecutor(1,
				(new Builder()).namingPattern(poolName + "-%d").daemon(false).build());
		this.initialDelay = initialDelay;
		this.period = period;
		this.timeUnit = timeUnit;
	}

	public void execute(Runnable task) {
		if (this.executorService != null) {
			this.executorService.scheduleAtFixedRate(task, this.initialDelay, this.period, this.timeUnit);
		} else {
			super.execute(task);
		}

	}
}
