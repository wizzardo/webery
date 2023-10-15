package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.EpollCore;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.http.HttpClient;
import com.wizzardo.tools.reflection.FieldReflection;
import com.wizzardo.tools.reflection.FieldReflectionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by wizzardo on 14/11/16.
 */
public class FallbackServerSocketTest {
    private FieldReflection epollSupportedField;
    private boolean epollSupport = EpollCore.SUPPORTED;

    {
        try {
            epollSupportedField = new FieldReflectionFactory().create(EpollCore.class, "SUPPORTED");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(epollSupportedField.getField(), epollSupportedField.getField().getModifiers() & ~Modifier.FINAL);
            epollSupportedField.getField().setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setup() {
        epollSupportedField.setBoolean(null, false);
    }


    @After
    public void shutdown() {
        epollSupportedField.setBoolean(null, epollSupport);
    }


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

    @Test
    public void test_slow_client() throws IOException, InterruptedException {
        System.out.println("EpollCore.SUPPORTED: " + EpollCore.SUPPORTED);

        byte[] data = new byte[1024 * 1024 * 10];
        new Random().nextBytes(data);

        AbstractHttpServer<HttpConnection> serverMock = new AbstractHttpServer(null, 9998, 2, true) {
            @Override
            protected void handle(HttpConnection connection) throws Exception {
                Request request = connection.getRequest();
                Response response = connection.getResponse();
                response.body(data);
            }
        };
        serverMock.setPort(9998);
        serverMock.start();

        Thread.sleep(20);
        com.wizzardo.tools.http.Response response = HttpClient.createRequest("http://localhost:" + serverMock.getPort() + "/").get();
        InputStream stream = response.asStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] result = new byte[response.getContentLength()];
        int offset = 0;
        while ((offset += stream.read(result, offset, Math.min(result.length - offset, 65536))) != result.length) {
            Thread.sleep(100);
        }

//        byte[] result = out.toByteArray();
//        byte[] result = IOTools.bytes(stream);
//        byte[] result = HttpClient.createRequest("http://localhost:" + serverMock.getPort() + "/").get().asBytes();
        Assert.assertArrayEquals(data, result);

        serverMock.close();
    }
}
