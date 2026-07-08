package com.ghbank.ifp.pool;

import java.util.List;

public interface IThreadPool {
	void execute(Runnable var1);

	<T> T submit(Runnable var1);

	void shutdown();

	List<Runnable> shutdownNow();
}
