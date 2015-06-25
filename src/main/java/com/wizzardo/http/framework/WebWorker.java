package com.wizzardo.http.framework;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.Worker;

import java.util.concurrent.BlockingQueue;

/**
 * Created by wizzardo on 28.04.15.
 */
public abstract class WebWorker<T extends HttpConnection> extends Worker<T> {
    protected RequestHolder requestHolder;
    protected String controller;
    protected String action;

    public WebWorker(BlockingQueue queue, String name) {
        super(queue, name);
    }

    public WebWorker(BlockingQueue queue) {
        super(queue);
    }

    public RequestHolder getRequestHolder() {
        return requestHolder;
    }

    public String controller() {
        return controller;
    }

    public String action() {
        return action;
    }
}
