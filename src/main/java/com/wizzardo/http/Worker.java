package com.wizzardo.http;


import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * @author: moxa
 * Date: 4/13/13
 */
public abstract class Worker<T> extends Thread implements ByteBufferProvider, Buffer {
    protected BlockingQueue<T> queue;
    protected ByteBufferWrapper byteBufferWrapper = new ByteBufferWrapper(1024 * 50);
    protected byte[] buffer = new byte[byteBufferWrapper.capacity()];
    protected int position;
    protected int limit;

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
            } catch (Exception e) {
                onError(e);
            }
        }
    }

    protected void onError(Exception e) {
        e.printStackTrace();
    }

    @Override
    public ByteBufferWrapper getBuffer() {
        return byteBufferWrapper;
    }

    protected abstract void process(T item) throws IOException;

    @Override
    public byte[] bytes() {
        return buffer;
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public void position(int position) {
        this.position = position;
    }

    @Override
    public int limit() {
        return limit;
    }

    @Override
    public void limit(int limit) {
        this.limit = limit;
    }

    @Override
    public int capacity() {
        return buffer.length;
    }

    @Override
    public boolean hasRemaining() {
        return position < limit;
    }

    @Override
    public int remains() {
        return limit - position;
    }

    @Override
    public void clear() {
        limit = 0;
        position = 0;
    }
}
