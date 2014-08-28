package com.wizzardo.http;

import com.wizzardo.epoll.EpollServer;
import com.wizzardo.epoll.IOThread;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: moxa
 * Date: 11/5/13
 */
public class HttpServer extends EpollServer<HttpConnection> {

    private BlockingQueue<HttpConnection> queue = new LinkedBlockingQueue<>();
    private int workersCount;

    public HttpServer(int port) {
        this(null, port);
    }

    public HttpServer(String host, int port) {
        this(host, port, 0);
    }

    public HttpServer(String host, int port, int workersCount) {
        super(host, port);
        this.workersCount = workersCount;
        System.out.println("worker count: " + workersCount);
        for (int i = 0; i < workersCount; i++) {
            new Worker(queue, "worker_" + i) {
                @Override
                protected void process(HttpConnection connection) {
                    handle(connection);
                }
            };
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
                connection.close();
                return;
            }

            if (workersCount > 0)
                queue.add(connection);
            else
                handle(connection);
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
        return staticResponse;
    }

    protected void handle(HttpConnection connection) {
        try {
            Response response = handleRequest(connection.getRequest());

            connection.reset();
            connection.write(response.toReadableBytes());
        } catch (Exception t) {
            t.printStackTrace();
            //TODO render error page
        }
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer(null, 8084, args.length > 0 ? Integer.parseInt(args[0]) : 16);
        server.setIoThreadsCount(args.length > 1 ? Integer.parseInt(args[1]) : 8);
        server.start();
    }
}
