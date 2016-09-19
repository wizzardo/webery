package com.wizzardo.http;

import com.wizzardo.epoll.IOThread;

/**
 * Created by wizzardo on 02.01.16.
 */
public class HttpIOThread<T extends HttpConnection> extends IOThread<T> {

    protected AbstractHttpServer<T> server;

    public HttpIOThread(AbstractHttpServer<T> server, int number, int divider) {
        super(number, divider);
        this.server = server;
    }

    @Override
    public void onRead(T connection) {
        server.process(connection, this);
    }

}
