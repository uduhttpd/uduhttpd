/*
 * Copyright (C) 2012 - 2016 nanohttpd 
 * Copyright (C) 2020 uduhttpd 
 *  
 * Copyright (C) 2020 uduhttpd
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.nanohttpd.util.concurrent;

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