package com.ghbank.ifp.pool;

import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class AbstractThreadPool implements IThreadPool {
	private ExecutorService threadPool;

	public ExecutorService getThreadPool() {
		return this.threadPool;
	}

	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	public void execute(Runnable task) {
		this.threadPool.execute(task);
	}

	public <T> T submit(Runnable task) {
		return (T) this.threadPool.submit(task);
	}

	public void shutdown() {
		this.threadPool.shutdown();
	}

	public List<Runnable> shutdownNow() {
		return this.threadPool.shutdownNow();
	}
}
