package com.wizzardo.http.framework;

import com.wizzardo.http.AbstractHttpServer;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.HttpIOThread;

/**
 * Created by wizzardo on 28.04.15.
 */
public class WebIOThread<T extends HttpConnection> extends HttpIOThread<T> implements RequestContext {
    protected RequestHolder requestHolder = new RequestHolder();
    protected String controller;
    protected String action;
    protected String handler;

    public WebIOThread(AbstractHttpServer<T> server, int number, int divider) {
        super(server, number, divider);
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
        requestHolder.reset();
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
