package com.wizzardo.http.framework;

public class DetachedRequestContext implements RequestContext {

    protected RequestHolder requestHolder = new RequestHolder();
    protected String controller;
    protected String action;
    protected String handler;

    public DetachedRequestContext(RequestContext context) {
        this.requestHolder.set(context.getRequestHolder());
        this.controller = context.controller();
        this.action = context.action();
        this.handler = context.handler();
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
