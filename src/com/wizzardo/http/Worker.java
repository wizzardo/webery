package com.wizzardo.http;

import simplehttpserver.RequestHolder;
import simplehttpserver.concurrent.NonBlockingQueue;

import java.nio.ByteBuffer;

/**
 * @author: moxa
 * Date: 4/13/13
 */
public class Worker extends Thread {
    protected NonBlockingQueue<Runnable> queue;
    protected RequestHolder requestHolder;
    protected final static int maxRequestSize = 1024 * 2;
    protected ByteBuffer buf = ByteBuffer.allocateDirect(maxRequestSize * 50);

    public Worker(NonBlockingQueue<Runnable> queue) {
        this(queue, "Worker");
    }

    public Worker(NonBlockingQueue<Runnable> queue, String name) {
        this.queue = queue;
        setDaemon(true);
        setName(name);
        start();
    }


    @Override
    public void run() {
        while (true) {
            queue.get().run();
        }
    }

    public RequestHolder getRequestHolder() {
        return requestHolder;
    }

    public void setRequestHolder(RequestHolder requestHolder) {
        this.requestHolder = requestHolder;
    }
}
