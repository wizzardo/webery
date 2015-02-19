package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.EpollServer;
import com.wizzardo.epoll.IOThread;
import com.wizzardo.epoll.readable.ReadableByteBuffer;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.response.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: moxa
 * Date: 11/5/13
 */
public abstract class AbstractHttpServer<T extends HttpConnection> extends EpollServer<T> {

    private ReadableByteBuffer staticResponse = new Response()
            .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
            .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_CONTENT_TYPE_HTML_UTF8)
            .setBody("It's alive!".getBytes())
            .buildStaticResponse();

    private BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private int workersCount;
    private int sessionTimeoutSec = 30 * 60;
    private volatile Handler handler = (request, response) -> response.setStaticResponse(staticResponse.copy());

    protected FiltersMapping filtersMapping = new FiltersMapping();

    public AbstractHttpServer(int port) {
        this(null, port);
    }

    public AbstractHttpServer(String host, int port) {
        this(host, port, 0);
    }

    public AbstractHttpServer(String host, int port, int workersCount) {
        super(host, port);
        this.workersCount = workersCount;

        System.out.println("worker count: " + workersCount);
        for (int i = 0; i < workersCount; i++) {
            new Worker<T>(queue, "worker_" + i) {
                @Override
                protected void process(T connection) {
                    processConnection(connection);
                }
            }.start();
        }
    }

    @Override
    public void run() {
        Session.createSessionsHolder(sessionTimeoutSec);
        super.run();
    }

    @Override
    protected T createConnection(int fd, int ip, int port) {
        return (T) new HttpConnection(fd, ip, port);
    }

    @Override
    protected IOThread<T> createIOThread(int number, int divider) {
        return new HttpIOThread(number, divider);
    }

    private class HttpIOThread extends IOThread<T> {

        public HttpIOThread(int number, int divider) {
            super(number, divider);
        }

        @Override
        public void onRead(T connection) {
            if (connection.processInputListener())
                return;

            ByteBuffer b;
            try {
                while ((b = read(connection, connection.getBufferSize(), this)).limit() > 0) {
                    if (connection.check(b))
                        break;
                }
                if (!connection.isRequestReady())
                    return;

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    connection.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }

            if (workersCount > 0)
                queue.add(connection);
            else
                processConnection(connection);
        }

        @Override
        public void onDisconnect(T connection) {
            super.onDisconnect(connection);
//            System.out.println("close " + connection);
        }
    }

    private void processConnection(T connection) {
        try {
            handle(connection);
            finishHandling(connection);
        } catch (Exception t) {
            t.printStackTrace();
            //TODO render error page
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract void handle(T connection);

    protected void finishHandling(T connection) throws IOException {
        connection.flushOutputStream();

        if (!connection.getResponse().isCommitted())
            connection.write(connection.getResponse().toReadableBytes(), (ByteBufferProvider) Thread.currentThread());

        connection.onFinishingHandling();
    }

    public FiltersMapping getFiltersMapping() {
        return filtersMapping;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setSessionTimeout(int sec) {
        this.sessionTimeoutSec = sec;
    }
}
