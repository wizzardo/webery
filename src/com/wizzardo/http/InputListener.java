package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public interface InputListener<C extends Connection> {
    public void onReadyToRead(C connection);

    public void onReady(C connection);
}
