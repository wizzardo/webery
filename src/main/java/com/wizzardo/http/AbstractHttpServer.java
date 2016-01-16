package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.EpollServer;
import com.wizzardo.epoll.IOThread;
import com.wizzardo.http.response.Status;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: moxa
 * Date: 11/5/13
 */
public abstract class AbstractHttpServer<T extends HttpConnection> extends EpollServer<T> {

    protected volatile BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    protected volatile int workersCount;
    protected volatile int sessionTimeoutSec = 30 * 60;
    protected String context;

    protected MimeProvider mimeProvider;

    public AbstractHttpServer() {
    }

    public AbstractHttpServer(int port) {
        this(null, port);
    }

    public AbstractHttpServer(String host, int port) {
        this(host, port, 0);
    }

    public AbstractHttpServer(String host, int port, int workersCount) {
        super(host, port);
        this.workersCount = workersCount;
    }

    protected Worker<T> createWorker(BlockingQueue<T> queue, String name) {
        return new HttpWorker<>(this, queue, name);
    }

    @Override
    public void run() {
        Session.createSessionsHolder(sessionTimeoutSec);
        System.out.println("worker count: " + workersCount);
        for (int i = 0; i < workersCount; i++) {
            createWorker(queue, "worker_" + i).start();
        }
        super.run();
    }

    @Override
    protected T createConnection(int fd, int ip, int port) {
        return (T) new HttpConnection(fd, ip, port, this);
    }

    @Override
    protected IOThread<T> createIOThread(int number, int divider) {
        return new HttpIOThread(this, number, divider);
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

        } catch (Exception e) {
            connection.setCloseOnFinishWriting(true);
            connection.getResponse().status(Status._400).commit(connection);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    void process(T connection, ByteBufferProvider bufferProvider) {
        if (workersCount > 0) {
            queue.add(connection);
        } else if (checkData(connection, bufferProvider))
            while (processConnection(connection)) {
            }
    }

    protected boolean processConnection(T connection) {
        try {
            handle(connection);
            return finishHandling(connection);
        } catch (Exception t) {
            try {
                onError(connection, t);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    protected abstract void handle(T connection) throws Exception;

    protected void onError(T connection, Exception e) {
        e.printStackTrace();
        //TODO render error page
    }

    protected boolean finishHandling(T connection) throws IOException {
        if (connection.getResponse().isAsync()) {
            if (connection.getInputListener() != null)
                connection.getInputListener().onReady(connection);

            return false;
        }

        connection.getResponse().commit(connection);
        connection.flushOutputStream();
        if (!connection.onFinishingHandling())
            return false;

        if (connection.isRequestReady())
            return true;
        else if (connection.isReadyToRead() && checkData(connection, (ByteBufferProvider) Thread.currentThread()))
            return true;
        return false;
    }

    public void setSessionTimeout(int sec) {
        checkIfStarted();
        this.sessionTimeoutSec = sec;
    }

    public void setWorkersCount(int count) {
        checkIfStarted();
        this.workersCount = count;
    }

    public void setContext(String context) {
        checkIfStarted();
        this.context = context;
    }

    public String getContext() {
        return context;
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
