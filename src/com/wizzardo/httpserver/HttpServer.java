package com.wizzardo.httpserver;

import com.wizzardo.epoll.EpollServer;
import com.wizzardo.httpserver.request.Header;
import com.wizzardo.httpserver.response.Response;
import simplehttpserver.concurrent.NonBlockingQueue;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author: moxa
 * Date: 11/5/13
 */
public class HttpServer extends EpollServer<HttpConnection> {

    private NonBlockingQueue<Runnable> queue = new NonBlockingQueue<Runnable>();

    public HttpServer(int port) {
        this(null, port);
    }

    public HttpServer(String host, int port) {
        super(host, port);
        for (int i = 0; i < 16; i++) {
            new Worker(queue, "worker_" + i);
        }
    }

    @Override
    protected HttpConnection createConnection(int fd, int ip, int port) {
        return new HttpConnection(this, fd, ip, port);
    }

    @Override
    public void onRead(final HttpConnection connection) {

        queue.add(new Runnable() {
            @Override
            public void run() {
                ByteBuffer b;
                try {

                    while ((b = read(connection, connection.getBufferSize())).limit() > 0) {
                        if (connection.check(b))
                            break;
                    }
                    if (!connection.isHttpReady())
                        return;

                } catch (IOException e) {
                    e.printStackTrace();
                    connection.reset("exception while reading");
                    close(connection);
                    return;
                }

                connection.reset("read headers");

                try {
                    Response response = handleRequest(connection);

                    connection.write(response.toReadableBytes());
                } catch (Throwable t) {
                    t.printStackTrace();
                    //TODO render error page
                }

                onWrite(connection);
            }
        });

    }
//    final byte[] bytes = FileTools.bytes(new File("/usr/share/nginx/html/resources/data/img.jpg"));

    public Response handleRequest(HttpConnection connection) {
        return new Response()
                .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
                .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_CONTENT_TYPE_HTML_UTF8)
                .setBody("It's alive!".getBytes());
//        return new Response()
//                .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
//                .appendHeader(Header.KEY_CONTENT_TYPE, "image/jpg")
//                .setBody(bytes);
    }


    @Override
    public void onConnect(HttpConnection httpConnection) {
//        System.out.println("new connection " + httpConnection);
    }

    @Override
    public void onDisconnect(HttpConnection httpConnection) {
//        System.out.println("close " + httpConnection);
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer(8084);
        server.start();
    }
}
