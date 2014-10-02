package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public interface RawHandler {
    public void onData(Connection connection);

    public void onReady(Connection connection);
}
