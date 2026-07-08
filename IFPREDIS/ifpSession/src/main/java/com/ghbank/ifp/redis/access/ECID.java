package com.ghbank.ifp.redis.access;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ECID {
	private static final int ERROR_COUNT_LIMIT = 100000;
	private Queue<RedisKey> ecQueue = new ConcurrentLinkedQueue();
	private int errorCount;

	public void add(RedisKey key) {
		if (this.errorCount < 100000) {
			this.ecQueue.add(key);
			this.counter(1);
		}

	}

	private synchronized void counter(int val) {
		this.errorCount += val;
	}

	public boolean isCleanable() {
		return this.errorCount >= 100000;
	}

	public RedisKey next() {
		RedisKey rk = (RedisKey) this.ecQueue.poll();
		if (rk != null) {
			this.counter(-1);
		}

		return rk;
	}

	public void clean() {
		synchronized (this.ecQueue) {
			this.ecQueue.clear();
			this.errorCount = 0;
		}
	}
}