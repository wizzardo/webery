package com.wizzardo.http;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public interface ConnectionListener {
    public void onData(HttpConnection connection);

    public void onReady(HttpConnection connection);
}
