package com.wizzardo.http;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * Created by wizzardo on 16.07.15.
 */
public class HttpWorker<T extends HttpConnection> extends Worker<T> {

    protected AbstractHttpServer<T> server;

    public HttpWorker(AbstractHttpServer<T> server, ThreadGroup group, BlockingQueue<T> queue, String name) {
        super(group, queue, name);
        this.server = server;
    }

    @Override
    protected void process(T connection) throws IOException {
        if (!connection.processingBy.compareAndSet(null, this))
            return;

        try {
            connection.process(this);
        } finally {
            connection.processingBy.set(null);
        }
    }
}
