package com.wizzardo.httpserver;

import com.wizzardo.epoll.EpollServer;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.httpserver.request.Header;
import com.wizzardo.httpserver.request.RequestHeaders;
import simplehttpserver.concurrent.NonBlockingQueue;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author: moxa
 * Date: 11/5/13
 */
public class HttpServer extends EpollServer<HttpConnection> {

    private NonBlockingQueue<Runnable> queue = new NonBlockingQueue<Runnable>();

    public HttpServer() {
        for (int i = 0; i < 16; i++) {
            new Worker(queue, "worker_" + i);
        }
    }

    @Override
    protected HttpConnection createConnection(int fd, int ip, int port) {
        return new HttpConnection(fd, ip, port);
    }

    @Override
    public void readyToRead(final HttpConnection connection) {

        queue.add(new Runnable() {
            @Override
            public void run() {
        ByteBuffer b;
        try {

            while ((b = read(connection, connection.getBufferSize())).limit() > 0) {
                if (!connection.check(b))
                    return;
            }

        } catch (IOException e) {
            e.printStackTrace();
            connection.reset("exception while reading");
            close(connection);
            return;
        }

        connection.reset("read headers");

//        queue.add(new Runnable() {
//            @Override
//            public void run() {
                String s = "HTTP/1.1 200 OK\r\nConnection: Keep-Alive\r\nContent-Length: 5\r\nContent-Type: text/html;charset=UTF-8\r\n\r\nololo";
//                String s = "HTTP/1.1 200 OK\r\nContent-Length: 5\r\nContent-Type: text/html;charset=UTF-8\r\n\r\nololo";
                connection.writeData = new ReadableByteArray(s.getBytes());

//                String s = "HTTP/1.1 200 OK\r\nConnection: Keep-Alive\r\nContent-Length: " + image.length + "\r\nContent-Type: image/jpg;charset=UTF-8\r\n\r\n";
//                state.writeData = new WriteBuffer();
//                state.writeData.bb = image;
//                state.writeData.add(s.getBytes());
//                state.writeData.add(image);

//                System.out.println(fd + " requested: " + state.requestCounter.incrementAndGet());
//                server.startWriting(fd);
                readyToWrite(connection);
            }
        });

    }


    @Override
    public void readyToWrite(final HttpConnection connection) {
        try {
            ReadableByteArray data = connection.writeData;
            RequestHeaders headers = connection.headers;
            if (data == null || data.isComplete()) {
                stopWriting(connection);
                readyToRead(connection);
            } else {
                while (!data.isComplete() && write(connection, data) > 0) {
                }
                if (!data.isComplete()) {
                    startWriting(connection);
                    return;
                }
                if (Header.VALUE_CONNECTION_CLOSE.equalsIgnoreCase(headers.get(Header.KEY_CONNECTION))) {
                    close(connection);
                    connection.reset("close. not keep-alive");
                }
            }
        } catch (IOException e) {
            close(connection);
            onCloseConnection(connection);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    @Override
    public void onOpenConnection(HttpConnection httpConnection) {
//        System.out.println("new connection " + httpConnection);
    }

    @Override
    public void onCloseConnection(HttpConnection httpConnection) {
//        System.out.println("close " + httpConnection);
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.bind(8084,1000);
        server.start();
    }
}
