package com.wizzardo.http;


import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;

import java.util.concurrent.BlockingQueue;

/**
 * @author: moxa
 * Date: 4/13/13
 */
public abstract class Worker<T> extends Thread implements ByteBufferProvider {
    protected BlockingQueue<T> queue;
    protected ByteBufferWrapper byteBufferWrapper = new ByteBufferWrapper(1024 * 50);

    public Worker(ThreadGroup group, BlockingQueue<T> queue) {
        this(group, queue, "Worker");
    }

    public Worker(ThreadGroup group, BlockingQueue<T> queue, String name) {
        super(group, name);
        this.queue = queue;
        setDaemon(true);
    }


    @Override
    public void run() {
        while (true) {
            try {
                process(queue.take());
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public ByteBufferWrapper getBuffer() {
        return byteBufferWrapper;
    }

    protected abstract void process(T item);
}
