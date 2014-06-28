package com.wizzardo.httpserver;

import com.wizzardo.epoll.EpollServer;
import com.wizzardo.epoll.IOThread;
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
    private int workersCount;

    public HttpServer(int port) {
        this(null, port);
    }

    public HttpServer(String host, int port) {
        this(host, port, 16);
    }

    public HttpServer(String host, int port, int workersCount) {
        super(host, port);
        this.workersCount = workersCount;
        System.out.println("worker count: " + workersCount);
        for (int i = 0; i < workersCount; i++) {
            new Worker(queue, "worker_" + i);
        }
    }

    @Override
    protected HttpConnection createConnection(int fd, int ip, int port) {
        return new HttpConnection(fd, ip, port);
    }

    @Override
    protected IOThread<HttpConnection> createIOThread() {
        return new HttpIOThread();
    }

    private class HttpIOThread extends IOThread<HttpConnection> {

        @Override
        public void onRead(final HttpConnection connection) {
            Runnable runnable = new Runnable() {
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
                        connection.close();
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
            };
            if (workersCount > 0)
                queue.add(runnable);
            else
                runnable.run();
        }

        @Override
        public void onConnect(HttpConnection httpConnection) {
//        System.out.println("new connection " + httpConnection);
        }

        @Override
        public void onDisconnect(HttpConnection httpConnection) {
//        System.out.println("close " + httpConnection);
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
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer(null, 8084, args.length > 0 ? Integer.parseInt(args[0]) : 16);
        server.start();
    }
}
