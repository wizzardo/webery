package com.wizzardo.http;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wizzardo on 20.06.15.
 */
public class PipeliningTest extends ServerTest {

    @Test
    public void test_1() throws IOException {
        handler = new UrlHandler()
                .append("/", (request, response) -> response.setBody("ok")) //length = 123
                .append("/foo", (request, response) -> response.setBody("foo")) //length = 124
                .append("/foobar", (request, response) -> response.setBody("foobar")) //length = 127
        ;

        Assert.assertTrue(response(request("/"), 123).endsWith("\r\n\r\nok"));

        String response = response(request("/") + request("/foo") + request("/foobar"), 123 + 124 + 127);
        Assert.assertTrue(response.contains("\r\n\r\nok"));
        Assert.assertTrue(response.contains("\r\n\r\nfoo"));
        Assert.assertTrue(response.endsWith("\r\n\r\nfoobar"));
    }

    protected String response(String request, int limit) {
        try {
            Socket socket = new Socket("localhost", port);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            byte[] response = new byte[limit];
            AtomicBoolean wait = new AtomicBoolean(true);
            new Thread(() -> {
                int r = 0;
                int total = 0;
                try {
                    while (total < limit && (r = in.read(response, total, limit - total)) != -1) {
                        total += r;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    assert false;
                }
                synchronized (PipeliningTest.this) {
                    wait.set(false);
                    PipeliningTest.this.notify();
                }
            }).start();
            out.write(request.getBytes());

            synchronized (PipeliningTest.this) {
                if (wait.get())
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
            return new String(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected String request(String path) {
        return "GET " + path + " HTTP/1.1\r\n" +
                "Host: localhost:" + port + "\r\n" +
                "Connection: Keep-Alive\r\n" +
                "\r\n";
    }

}
