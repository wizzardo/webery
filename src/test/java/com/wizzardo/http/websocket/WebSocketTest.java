package com.wizzardo.http.websocket;

import com.wizzardo.http.ServerTest;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: wizzardo
 * Date: 09.10.14
 */
public class WebSocketTest extends ServerTest {

    @Test
    public void echoTest() throws IOException, URISyntaxException, InterruptedException {
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                listener.sendMessage(message);
            }
        };

        AtomicReference<String> messageHolder = new AtomicReference<>();
        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort()) {
            @Override
            public void onMessage(Message message) {
                messageHolder.set(message.asString());
            }
        };
        client.send("foo bar");
        client.waitForMessage();
        Assert.assertEquals("foo bar", messageHolder.get());
    }

    @Test
    public void testFrames() throws IOException, URISyntaxException, InterruptedException {
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                listener.sendMessage(message);
            }
        };

        AtomicReference<String> messageHolder = new AtomicReference<>();
        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort()) {
            @Override
            public void onMessage(Message message) {
                messageHolder.set(message.asString());
            }
        };
        Message message = new Message().append("foo").append(" ").append("bar");
        client.send(message);
        client.waitForMessage();
        Assert.assertEquals("foo bar", messageHolder.get());
    }

    @Test
    public void echoDifferentSizeTest() throws IOException, URISyntaxException, InterruptedException {
        server.setWebsocketFrameLengthLimit(1024 * 1024);
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                listener.sendMessage(message);
            }
        };

        AtomicReference<String> md5Holder = new AtomicReference<>();
        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort()) {
            @Override
            public void onMessage(Message message) {
                md5Holder.set(MD5.create().update(message.asBytes()).asString());
            }
        };
        byte[] data = new byte[126];
        ThreadLocalRandom.current().nextBytes(data);
        client.send(data);
        client.waitForMessage();
        Assert.assertEquals(MD5.create().update(data).asString(), md5Holder.get());

        data = new byte[64 * 1024];
        ThreadLocalRandom.current().nextBytes(data);
        client.send(data);
        client.waitForMessage();
        Assert.assertEquals(MD5.create().update(data).asString(), md5Holder.get());

        data = new byte[1024 * 1024];
        ThreadLocalRandom.current().nextBytes(data);
        client.send(data);
        client.waitForMessage();
        Assert.assertEquals(MD5.create().update(data).asString(), md5Holder.get());
    }

    @Test
    public void closeTest() throws IOException, URISyntaxException, InterruptedException {
        AtomicReference<String> messageHolder = new AtomicReference<>();
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                if ("close".equals(message.asString()))
                    listener.close();
            }

            @Override
            public void onDisconnect(WebSocketListener listener) {
                Assert.assertTrue(messageHolder.compareAndSet(null, "closed"));
            }
        };

        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort());
        client.send("close");
        client.waitForMessage();
        Assert.assertTrue(client.isClosed());
        Assert.assertEquals("closed", messageHolder.get());

        messageHolder.set(null);
        client = new SimpleWebSocketClient("ws://localhost:" + getPort());
        client.close();
        Thread.sleep(10);
        Assert.assertTrue(client.isClosed());
        Assert.assertEquals("closed", messageHolder.get());
    }

    @Test
    public void closeTest_2() throws IOException, URISyntaxException, InterruptedException {
        AtomicReference<String> messageHolder = new AtomicReference<>();
        handler = new WebSocketHandler() {
            @Override
            public void onDisconnect(WebSocketListener listener) {
                Assert.assertTrue(messageHolder.compareAndSet(null, "closed"));
            }
        };

        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort());
        client.in.close();
        Thread.sleep(10);
        Assert.assertEquals("closed", messageHolder.get());
    }

    @Test
    public void pingTest() throws IOException, URISyntaxException, InterruptedException {
        handler = new WebSocketHandler();

        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort());

        Assert.assertTrue(client.ping() >= 0);
    }
}
