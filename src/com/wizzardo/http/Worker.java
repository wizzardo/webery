package com.wizzardo.http;


import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;

import java.util.concurrent.BlockingQueue;

/**
 * @author: moxa
 * Date: 4/13/13
 */
public abstract class Worker<T extends HttpConnection> extends Thread implements ByteBufferProvider {
    protected BlockingQueue<T> queue;
    protected ByteBufferWrapper byteBufferWrapper = new ByteBufferWrapper(1024 * 50);

    public Worker(BlockingQueue<T> queue) {
        this(queue, "Worker");
    }

    public Worker(BlockingQueue<T> queue, String name) {
        this.queue = queue;
        setDaemon(true);
        setName(name);
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

    protected abstract void process(T connection);
}
