package com.wizzardo.http;


import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

/**
 * @author: moxa
 * Date: 4/13/13
 */
public abstract class Worker extends Thread {
    protected BlockingQueue<HttpConnection> queue;
    protected final static int maxRequestSize = 1024 * 2;
    protected ByteBuffer buf = ByteBuffer.allocateDirect(maxRequestSize * 50);

    public Worker(BlockingQueue<HttpConnection> queue) {
        this(queue, "Worker");
    }

    public Worker(BlockingQueue<HttpConnection> queue, String name) {
        this.queue = queue;
        setDaemon(true);
        setName(name);
        start();
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

    protected abstract void process(HttpConnection connection);
}
