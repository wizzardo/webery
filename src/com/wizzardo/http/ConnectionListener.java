package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public interface ConnectionListener {
    public void onData(Connection connection);

    public void onReady(Connection connection);
}
