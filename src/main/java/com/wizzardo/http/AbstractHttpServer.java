package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.EpollServer;
import com.wizzardo.epoll.IOThread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: moxa
 * Date: 11/5/13
 */
public abstract class AbstractHttpServer<T extends HttpConnection> extends EpollServer<T> {

    private BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private int workersCount;
    private int sessionTimeoutSec = 30 * 60;

    protected MimeProvider mimeProvider;

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
            createWorker(queue, "worker_" + i).start();
        }
    }

    protected Worker<T> createWorker(BlockingQueue<T> queue, String name) {
        return new Worker<T>(queue, name) {
            @Override
            protected void process(T connection) {
                if (!connection.processingBy.compareAndSet(null, this))
                    return;

                processConnection(connection);
                connection.processingBy.set(null);
            }
        };
    }

    @Override
    public void run() {
        Session.createSessionsHolder(sessionTimeoutSec);
        super.run();
    }

    @Override
    protected T createConnection(int fd, int ip, int port) {
        return (T) new HttpConnection(fd, ip, port, this);
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

            if (connection.processingBy.get() != null)
                return;

            if (checkData(connection, this))
                process(connection);
        }

        @Override
        public void onDisconnect(T connection) {
            super.onDisconnect(connection);
//            System.out.println("close " + connection);
        }
    }

    protected boolean checkData(T connection, ByteBufferProvider bufferProvider) {
        ByteBuffer b;
        try {
            while ((b = connection.read(connection.getBufferSize(), bufferProvider)).limit() > 0) {
                if (connection.check(b))
                    break;
            }
            if (!connection.isRequestReady())
                return false;

        } catch (IOException e) {
            e.printStackTrace();
            try {
                connection.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return false;
        }

        return true;
    }

    private void process(T connection) {
        if (workersCount > 0) {
            queue.add(connection);
        } else
            processConnection(connection);
    }

    protected void processConnection(T connection) {
        try {
            handle(connection);
            finishHandling(connection);
        } catch (Exception t) {
            try {
                onError(connection, t);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract void handle(T connection) throws Exception;

    protected void onError(T connection, Exception e) {
        e.printStackTrace();
        //TODO render error page
    }

    protected void finishHandling(T connection) throws IOException {
        if (connection.getResponse().isAsync())
            return;

        connection.getResponse().commit(connection);
        connection.flushOutputStream();
        connection.onFinishingHandling();

        if (connection.isRequestReady())
            processConnection(connection);
        else if (connection.isReadyToRead() && checkData(connection, (ByteBufferProvider) Thread.currentThread()))
            processConnection(connection);
    }

    public void setSessionTimeout(int sec) {
        this.sessionTimeoutSec = sec;
    }

    public MimeProvider getMimeProvider() {
        if (mimeProvider == null)
            mimeProvider = createMimeProvider();

        return mimeProvider;
    }

    protected MimeProvider createMimeProvider() {
        return new MimeProvider();
    }
}
