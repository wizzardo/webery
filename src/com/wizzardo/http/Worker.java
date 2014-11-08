package com.wizzardo.http;


import java.util.concurrent.BlockingQueue;

/**
 * @author: moxa
 * Date: 4/13/13
 */
public abstract class Worker<T extends HttpConnection> extends Thread {
    protected BlockingQueue<T> queue;

    public Worker(BlockingQueue<T> queue) {
        this(queue, "Worker");
    }

    public Worker(BlockingQueue<T> queue, String name) {
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

    protected abstract void process(T connection);
}
