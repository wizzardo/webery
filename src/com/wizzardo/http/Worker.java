package com.wizzardo.http;


import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

/**
 * @author: moxa
 * Date: 4/13/13
 */
public class Worker extends Thread {
    protected BlockingQueue<Runnable> queue;
    protected final static int maxRequestSize = 1024 * 2;
    protected ByteBuffer buf = ByteBuffer.allocateDirect(maxRequestSize * 50);

    public Worker(BlockingQueue<Runnable> queue) {
        this(queue, "Worker");
    }

    public Worker(BlockingQueue<Runnable> queue, String name) {
        this.queue = queue;
        setDaemon(true);
        setName(name);
        start();
    }


    @Override
    public void run() {
        while (true) {
            try {
                queue.take().run();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
