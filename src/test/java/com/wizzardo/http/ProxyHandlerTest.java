package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wizzardo on 24.06.15.
 */
public class ProxyHandlerTest extends ServerTest {

    AtomicInteger pauses = new AtomicInteger(0);

    class ObservedConnection extends HttpConnection {

        public ObservedConnection(int fd, int ip, int port, AbstractHttpServer server) {
            super(fd, ip, port, server);
        }

        @Override
        protected boolean actualWrite(ReadableData readable, ByteBufferProvider bufferProvider) throws IOException {
            boolean b = super.actualWrite(readable, bufferProvider);
            if (!b && !readable.isComplete())
                pauses.incrementAndGet();

            return b;
        }
    }

    @Test
    public void test_lazy_writing() {
        byte[] data = new byte[10 * 1024 * 1024];
        ThreadLocalRandom.current().nextBytes(data);

        handler = new UrlHandler()
                .append("/", (request, response) -> response.setBody(data));

        HttpServer<ObservedConnection> proxy = new HttpServer<ObservedConnection>(null, port + 1, context, 0) {
            @Override
            protected ObservedConnection createConnection(int fd, int ip, int port) {
                return new ObservedConnection(fd, ip, port, this);
            }
        };
        proxy.getUrlMapping()
                .append("/", new ProxyHandler("localhost", port));

        proxy.setIoThreadsCount(1);
        proxy.start();

        try {
            Assert.assertTrue(writeSocket(data, 0) == 0);
            Assert.assertTrue(writeSocket(data, 1) > 10);
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        } finally {
            proxy.close();
        }
    }

    protected int writeSocket(byte[] data, int pause) throws IOException, InterruptedException {
        int offset = 120 + String.valueOf(data.length).length();
        byte[] bytes = new byte[data.length + offset];
        int r, total = 0;
        Socket s = new Socket("localhost", port + 1);
        try {
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            out.write(("GET / HTTP/1.1\r\nHost:localhost:" + (port + 1) + "\r\nConnection:Keep-alive\r\n\r\n").getBytes());
            out.flush();

            while (bytes.length - total > 0 && (r = in.read(bytes, total, Math.min(bytes.length - total, 128 * 1024))) != -1) {
                total += r;
//                System.out.println("read:" + total+"\t remains:"+(bytes.length - total));
                if (pause > 0)
                    Thread.sleep(pause);
            }
        } finally {
            s.close();
        }

        Assert.assertEquals(MD5.create().update(data).asString(), MD5.create().update(bytes, offset, bytes.length - offset).asString());
        return pauses.getAndSet(0);
    }

    @Test
    public void test_simple() {
        handler = new UrlHandler()
                .append("/", (request, response) -> response.setBody("ok"));

        HttpServer<ObservedConnection> proxy = new HttpServer<ObservedConnection>(null, port + 1, context, 0) {
            @Override
            protected ObservedConnection createConnection(int fd, int ip, int port) {
                return new ObservedConnection(fd, ip, port, this);
            }
        };
        proxy.getUrlMapping()
                .append("/", new ProxyHandler("localhost", port));

        proxy.setIoThreadsCount(1);
        proxy.start();

        try {
            Assert.assertEquals("ok", makeRequest("/").header("Connection", "Close").get().asString());
            Assert.assertEquals("ok", makeRequest("/").header("Connection", "Keep-Alive").get().asString());
            Assert.assertEquals("ok", makeRequest("/", port + 1).header("Connection", "Close").get().asString());
            Assert.assertEquals("ok", makeRequest("/", port + 1).header("Connection", "Keep-Alive").get().asString());
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        } finally {
            proxy.close();
        }
    }

    @Test
    public void test_params() {
        handler = new UrlHandler()
                .append("/", (request, response) -> response.setBody(request.param("foo") + " " + request.param("bar")));

        HttpServer<HttpConnection> proxy = new HttpServer<>(null, port + 1, context, 0);
        proxy.getUrlMapping()
                .append("/", new ProxyHandler("localhost", port));

        proxy.setIoThreadsCount(1);
        proxy.start();

        try {
            Assert.assertEquals("null null", makeRequest("/", port + 1).get().asString());
            Assert.assertEquals("foo null", makeRequest("/", port + 1).param("foo", "foo").get().asString());
            Assert.assertEquals("null bar", makeRequest("/", port + 1).param("bar", "bar").get().asString());
            Assert.assertEquals("foo bar", makeRequest("/", port + 1).param("foo", "foo").param("bar", "bar").get().asString());
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        } finally {
            proxy.close();
        }
    }

}
