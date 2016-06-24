package com.wizzardo.http.framework;

/**
 * Created by wizzardo on 02.01.16.
 */
public interface RequestContext {
    RequestHolder getRequestHolder();

    String controller();

    String action();

    void setRequestHolder(RequestHolder requestHolder);

    void setController(String controller);

    void setAction(String action);

    static RequestContext get() {
        return (RequestContext) Thread.currentThread();
    }
}
