package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public interface InputListener<C extends Connection> extends Closeable {
    void onReadyToRead(C connection);

    default void onReady(C connection) {
    }

    default void close() throws IOException {
    }
}
