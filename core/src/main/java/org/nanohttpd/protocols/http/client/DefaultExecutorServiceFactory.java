package org.nanohttpd.protocols.http.client;

import java.util.concurrent.*;

public class DefaultExecutorServiceFactory implements ExecutorServiceFactory {
    @Override
    public ExecutorService create() {
        return new ThreadPoolExecutor(3, 40, 5, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
    }
}
