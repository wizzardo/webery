package com.wizzardo.http.framework;

import com.wizzardo.http.AbstractHttpServer;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.HttpWorker;

import java.util.concurrent.BlockingQueue;

/**
 * Created by wizzardo on 28.04.15.
 */
public class WebWorker<T extends HttpConnection> extends HttpWorker<T> implements RequestContext {
    protected RequestHolder requestHolder;
    protected String controller;
    protected String action;
    protected String handler;

    public WebWorker(AbstractHttpServer<T> server, ThreadGroup group, BlockingQueue<T> queue, String name) {
        super(server, group, queue, name);
    }

    @Override
    public RequestHolder getRequestHolder() {
        return requestHolder;
    }

    @Override
    public String controller() {
        return controller;
    }

    @Override
    public String action() {
        return action;
    }

    @Override
    public void setRequestHolder(RequestHolder requestHolder) {
        this.requestHolder = requestHolder;
    }

    @Override
    public void setController(String controller) {
        this.controller = controller;
    }

    @Override
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public void reset() {
        action = null;
        controller = null;
        requestHolder = null;
    }

    @Override
    public void handler(String name) {
        this.handler = name;
    }

    @Override
    public String handler() {
        return handler;
    }
}
