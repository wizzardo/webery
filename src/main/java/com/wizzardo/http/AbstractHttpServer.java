package com.wizzardo.http;

import com.wizzardo.epoll.*;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.response.Status;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: moxa
 * Date: 11/5/13
 */
public abstract class AbstractHttpServer<T extends HttpConnection> {

    protected volatile BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    protected volatile int workersCount;
    protected volatile int sessionTimeoutSec = 30 * 60;
    protected String context;
    protected final EpollServer<T> server;

    protected MimeProvider mimeProvider;

    public AbstractHttpServer() {
        this(null, 8080);
    }

    public AbstractHttpServer(int port) {
        this(null, port);
    }

    public AbstractHttpServer(String host, int port) {
        this(host, port, 0);
    }

    public AbstractHttpServer(String host, int port, int workersCount) {
        if (EpollCore.SUPPORTED) {
            server = new EpollServer<T>(host, port) {
                @Override
                protected T createConnection(int fd, int ip, int port) {
                    return AbstractHttpServer.this.createConnection(fd, ip, port);
                }

                @Override
                protected IOThread<T> createIOThread(int number, int divider) {
                    return AbstractHttpServer.this.createIOThread(number, divider);
                }
            };
        } else {
            server = new FallbackServerSocket<T>(host, port) {
                @Override
                public void onRead(T connection, ByteBufferProvider bufferProvider) {
                    if (connection.processInputListener())
                        return;
                    process(connection, bufferProvider);
                }

                @Override
                protected SelectorConnectionWrapper createConnection(SocketChannel client) throws IOException {
                    SelectorConnectionWrapper connection = super.createConnection(client);
                    connection.server = AbstractHttpServer.this;
                    return connection;
                }
            };
        }
        this.workersCount = workersCount;
    }

    protected Worker<T> createWorker(BlockingQueue<T> queue, String name) {
        return new HttpWorker<>(this, queue, name);
    }

    public synchronized void start() {
        run();
    }

    public void run() {
        Session.createSessionsHolder(sessionTimeoutSec);
        System.out.println("worker count: " + workersCount);
        for (int i = 0; i < workersCount; i++) {
            createWorker(queue, "worker_" + i).start();
        }
        server.start();
    }

    public int getPort() {
        return server.getPort();
    }

    public String getNetworkInterface() {
        return server.getNetworkInterface();
    }

    public boolean isSecured() {
        return false;
    }

    public void setNetworkInterface(String networkInterface) {
        server.setNetworkInterface(networkInterface);
    }

    public void setPort(int port) {
        server.setPort(port);
    }

    public void setIoThreadsCount(int ioThreadsCount) {
        server.setIoThreadsCount(ioThreadsCount);
    }

    public void setTTL(long milliseconds) {
        server.setTTL(milliseconds);
    }

    public void loadCertificates(SslConfig sslConfig) {
        server.loadCertificates(sslConfig);
    }

    public void loadCertificates(String certFile, String keyFile) {
        server.loadCertificates(certFile, keyFile);
    }

    public void close() {
        server.close();
    }

    protected T createConnection(int fd, int ip, int port) {
        return (T) new HttpConnection(fd, ip, port, this);
    }

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

        } catch (HttpException e) {
            closeConnection(connection, e.status);
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            closeConnection(connection, Status._400);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    protected void closeConnection(T connection, Status status) {
        connection.setCloseOnFinishWriting(true);
        connection.getResponse()
                .status(status)
                .appendHeader(Header.KV_CONNECTION_CLOSE)
                .commit(connection, getBufferProvider());
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

        connection.getResponse().commit(connection, getBufferProvider());
        connection.flushOutputStream();
        if (!connection.onFinishingHandling())
            return false;

        if (connection.isRequestReady())
            return true;
        else if (connection.isReadyToRead() && checkData(connection, getBufferProvider()))
            return true;
        return false;
    }

    protected ByteBufferProvider getBufferProvider() {
        return (ByteBufferProvider) Thread.currentThread();
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

    protected void checkIfStarted() {
        if (server.isStarted())
            throw new IllegalStateException("Server is already started");
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
