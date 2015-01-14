package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public interface OutputListener<C extends Connection> {
    public void onReadyToWrite(C connection);

    default public void onReady(C connection) {
    }
}
