package com.wizzardo.http.framework;

import com.wizzardo.http.AbstractHttpServer;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.HttpWorker;

import java.util.concurrent.BlockingQueue;

/**
 * Created by wizzardo on 28.04.15.
 */
public class WebWorker<T extends HttpConnection> extends HttpWorker<T> {
    protected RequestHolder requestHolder;
    protected String controller;
    protected String action;

    public WebWorker(AbstractHttpServer<T> server, BlockingQueue<T> queue, String name) {
        super(server, queue, name);
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
