package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public interface InputListener<C extends Connection> {
    void onReadyToRead(C connection);

    default void onReady(C connection) {
    }

    default void onDisconnect(C connection) {
    }
}
