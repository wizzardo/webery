package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

import java.io.Closeable;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public interface InputListener<C extends Connection> extends Closeable {
    void onReadyToRead(C connection);

    default void onReady(C connection) {
    }
}
