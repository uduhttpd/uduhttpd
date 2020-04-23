package org.nanohttpd.concurrent.util;

import java.util.List;

/**
 * Take on a runnable to add it to a given list when it starts and remove it when that runnable completes its lifecycle.
 * This is supposed to be used with {@link java.util.concurrent.Executor} and similar to provide the flexibility to
 * manage the running tasks, and you don't want to ensure that the given runnable will register itself to the given
 * list.
 * @param <T> Type of runnable that will be saved to the list.
 */
public class RegistrarRunnable<T extends Runnable> implements Runnable {
    private final List<? super T> list;
    private final T runnable;

    /**
     * Creates an instance of this class that will soon be fed to the executors or started directly from a thread
     * instance.
     * @param runnable that will be undertaken.
     * @param list to where we will hold the given runnable instance while it is running.
     */
    public RegistrarRunnable(T runnable, List<? super T> list) {
        this.runnable = runnable;
        this.list = list;
    }

    @Override
    public void run() {
        list.add(runnable);
        runnable.run();
        list.remove(runnable);
    }
}