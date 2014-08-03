package com.wizzardo.httpserver;

import com.wizzardo.epoll.EpollServer;
import com.wizzardo.epoll.IOThread;
import com.wizzardo.httpserver.request.Header;
import com.wizzardo.httpserver.request.Request;
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
            final Runnable write = new Runnable() {
                @Override
                public void run() {
                    try {
                        Response response = handleRequest(connection.getRequest());

                        connection.reset("end");
                        connection.write(response.toReadableBytes());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        //TODO render error page
                    }
                }
            };

            Runnable read = new Runnable() {
                @Override
                public void run() {
                    if (connection.getState() == HttpConnection.State.READING_INPUT_STREAM) {
                        connection.getInputStream().wakeUp();
                        return;
                    }

                    ByteBuffer b;
                    try {

                        while ((b = read(connection, connection.getBufferSize())).limit() > 0) {
                            if (connection.check(b))
                                break;
                        }
                        if (!connection.isRequestReady())
                            return;

                    } catch (IOException e) {
                        e.printStackTrace();
                        connection.reset("exception while reading");
                        connection.close();
                        return;
                    }

//                    RequestStats rs = requestStats.get(connection.getHeadersReader().getPath());
//                    if (workersCount > 0 && (rs == null || rs.duration > 3))
                    queue.add(write);
//                    else
//                    write.run();
                }
            };

//            if (workersCount > 0)
//                queue.add(read);
////            else
            read.run();
        }

        @Override
        public void onConnect(HttpConnection httpConnection) {
//        System.out.println("new connection " + httpConnection);
        }

        @Override
        public void onDisconnect(HttpConnection httpConnection) {
//        System.out.println("close " + httpConnection);
        }
    }


//        final byte[] bytes = FileTools.bytes(new File("/usr/share/nginx/html/resources/data/img.jpg"));

    private Response staticResponse = new Response()
            .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
            .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_CONTENT_TYPE_HTML_UTF8)
            .setBody("It's alive!".getBytes())
            .makeStatic();
//        private Response staticResponse = new Response()
//                .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
//                .appendHeader(Header.KEY_CONTENT_TYPE, "image/jpg")
//                .setBody(bytes)
//                .makeStatic();

    public Response handleRequest(Request request) {
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException ignored) {
//            }
//            return new Response()
//                    .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
//                    .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_CONTENT_TYPE_HTML_UTF8)
//                    .setBody("It's alive!".getBytes());
//        return new Response()
//                .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
//                .appendHeader(Header.KEY_CONTENT_TYPE, "image/jpg")
//                .setBody(bytes);
        return staticResponse;
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer(null, 8084, args.length > 0 ? Integer.parseInt(args[0]) : 16);
        server.setIoThreadsCount(args.length > 1 ? Integer.parseInt(args[1]) : 8);
        server.start();
    }
}
