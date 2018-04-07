package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.http.response.Response;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by wizzardo on 14/11/16.
 */
public class FallbackServerSocketTest {
    @Test
    public void test_simple_read() throws IOException, InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        AbstractHttpServer<HttpConnection> serverMock = new AbstractHttpServer(null, 9999, 2, true) {
            @Override
            protected void handle(HttpConnection connection) throws Exception {
            }
        };

        FallbackServerSocket serverSocket = new FallbackServerSocket() {
            @Override
            protected SelectorConnectionWrapper createConnection(SocketChannel client) throws IOException {
                return new SelectorConnectionWrapper(client, serverMock);
            }

            @Override
            public void onRead(HttpConnection connection, ByteBufferProvider bufferProvider) {
                byte[] bytes = new byte[1024];
                try {
                    int read = connection.read(bytes, bufferProvider);
                    if (read > 0) {
                        counter.incrementAndGet();
                        Assert.assertEquals("foobar", new String(bytes, 0, read));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        serverSocket.setPort(9999);
        serverSocket.start();

        Thread.sleep(20);

        Socket socket = new Socket("localhost", serverSocket.getPort());
        socket.getOutputStream().write("foobar".getBytes());
        socket.getOutputStream().flush();
        socket.close();
        Thread.sleep(300);

        serverSocket.close();
        Assert.assertEquals(1, counter.get());
    }

    @Test
    public void test_simple_write() throws IOException, InterruptedException {
        AbstractHttpServer<HttpConnection> serverMock = new AbstractHttpServer(null, 9999, 2, true) {
            @Override
            protected void handle(HttpConnection connection) throws Exception {
            }
        };
        FallbackServerSocket serverSocket = new FallbackServerSocket() {
            @Override
            protected SelectorConnectionWrapper createConnection(SocketChannel client) throws IOException {
                return new SelectorConnectionWrapper(client, serverMock);
            }

            @Override
            public void onRead(HttpConnection connection, ByteBufferProvider bufferProvider) {
                byte[] bytes = new byte[1024];
                try {
                    int read = connection.read(bytes, bufferProvider);
                    if (read > 0) {
                        connection.write(bytes, 0, read, bufferProvider);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        serverSocket.setPort(9999);
        serverSocket.start();

        Thread.sleep(20);

        Socket socket = new Socket("localhost", serverSocket.getPort());
        socket.getOutputStream().write("foobar".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(300);
        byte[] bytes = new byte[1024];
        int read = socket.getInputStream().read(bytes);
        Assert.assertEquals("foobar", new String(bytes, 0, read));
        socket.close();

        serverSocket.close();
    }
}
