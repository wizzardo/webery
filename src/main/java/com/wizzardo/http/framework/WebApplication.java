package com.wizzardo.http.framework;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.HttpServer;
import com.wizzardo.http.Worker;

import java.util.concurrent.BlockingQueue;

/**
 * Created by wizzardo on 28.04.15.
 */
public class WebApplication<T extends HttpConnection> extends HttpServer<T> {

    public WebApplication(int port) {
        super(port);
    }

    public WebApplication(String host, int port) {
        super(host, port);
    }

    public WebApplication(String host, int port, String context) {
        super(host, port, context);
    }

    public WebApplication(String host, int port, int workersCount) {
        super(host, port, workersCount);
    }

    public WebApplication(String host, int port, String context, int workersCount) {
        super(host, port, context, workersCount);
    }

    @Override
    protected Worker<T> createWorker(BlockingQueue<T> queue, String name) {
        return new WebWorker<T>(queue, name) {
            @Override
            protected void process(T connection) {
                processConnection(connection);
            }
        };
    }
}
