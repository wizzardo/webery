package com.wizzardo.http.websocket;

import com.wizzardo.http.ServerTest;
import com.wizzardo.tools.interfaces.Supplier;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: wizzardo
 * Date: 09.10.14
 */
public class WebSocketTest extends ServerTest {

    @Override
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        workers = 0;
        super.setUp();
    }

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
    public void limitTest() throws IOException, URISyntaxException, InterruptedException {
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                listener.sendMessage(message);
            }
        };

        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort()) {
            @Override
            public void onMessage(Message message) {
                throw new IllegalArgumentException();
            }
        };
        byte[] data;

        data = new byte[1024 * 1024];
        ThreadLocalRandom.current().nextBytes(data);
        try {
            client.send(data);
            client.waitForMessage();
            Assert.assertTrue(false);
        } catch (SocketException e) {
            Assert.assertEquals("Connection reset", e.getMessage());
        }
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
        try {
            client.waitForMessage();
            Assert.assertTrue(client.isClosed());
        } catch (SocketException e) {
            Assert.assertEquals("Connection reset", e.getMessage());
        }
        Thread.sleep(10);
        Assert.assertEquals("closed", messageHolder.get());

        messageHolder.set(null);
        client = new SimpleWebSocketClient("ws://localhost:" + getPort());
        client.connectIfNot();
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
        client.connectIfNot();
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

    @Test
    public void test_buffers_pool() throws IOException, URISyntaxException, InterruptedException {
        server.setWebsocketFrameLengthLimit(1024 * 1024);
        AtomicInteger counterCreateBuffer = new AtomicInteger();
        AtomicInteger counterIncreaseBufferSize = new AtomicInteger();
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                listener.sendMessage(message);
            }

            @Override
            protected Supplier<ByteArrayHolder> createByteArraySupplier() {
                return () -> new ByteArrayHolder() {
                    {
                        counterCreateBuffer.incrementAndGet();
                    }

                    @Override
                    protected void createArray(int minLength) {
                        super.createArray(minLength);
                        counterIncreaseBufferSize.incrementAndGet();
                    }
                };
            }

            @Override
            public void onDisconnect(WebSocketListener listener) {
                System.out.println("onDisconnect");
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
        for (int i = 0; i < 100; i++) {
            ThreadLocalRandom.current().nextBytes(data);
            client.send(data);
            client.waitForMessage();
            Assert.assertEquals(MD5.create().update(data).asString(), md5Holder.get());
        }

        Assert.assertEquals(1, counterCreateBuffer.get());
        Assert.assertEquals(0, counterIncreaseBufferSize.get());
    }

    @Test
    public void test_buffers_pool_bigger_message() throws IOException, URISyntaxException, InterruptedException {
        server.setWebsocketFrameLengthLimit(1024 * 1024);
        AtomicInteger counterCreateBuffer = new AtomicInteger();
        AtomicInteger counterIncreaseBufferSize = new AtomicInteger();
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                listener.sendMessage(message);
            }

            @Override
            protected Supplier<ByteArrayHolder> createByteArraySupplier() {
                return () -> new ByteArrayHolder() {
                    {
                        counterCreateBuffer.incrementAndGet();
                    }

                    @Override
                    protected void createArray(int minLength) {
                        super.createArray(minLength);
                        counterIncreaseBufferSize.incrementAndGet();
                    }
                };
            }
        };

        AtomicReference<String> md5Holder = new AtomicReference<>();
        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort()) {
            @Override
            public void onMessage(Message message) {
                md5Holder.set(MD5.create().update(message.asBytes()).asString());
            }
        };
        byte[] data = new byte[20480];
        for (int i = 0; i < 10; i++) {
            ThreadLocalRandom.current().nextBytes(data);
            client.send(data);
            client.waitForMessage();
            Assert.assertEquals(MD5.create().update(data).asString(), md5Holder.get());
        }
        Assert.assertEquals(1, counterCreateBuffer.get());
        Assert.assertEquals(1, counterIncreaseBufferSize.get());
    }

    @Test
    public void test_buffers_pool_several_clients() throws IOException, URISyntaxException, InterruptedException {
        server.setWebsocketFrameLengthLimit(1024 * 1024);
        AtomicInteger counterCreateBuffer = new AtomicInteger();
        AtomicInteger counterIncreaseBufferSize = new AtomicInteger();
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                listener.sendMessage(message);
            }

            @Override
            protected Supplier<ByteArrayHolder> createByteArraySupplier() {
                return () -> new ByteArrayHolder() {
                    {
                        counterCreateBuffer.incrementAndGet();
                    }

                    @Override
                    protected void createArray(int minLength) {
                        super.createArray(minLength);
                        counterIncreaseBufferSize.incrementAndGet();
                    }
                };
            }

            @Override
            public void onDisconnect(WebSocketListener listener) {
                System.out.println("onDisconnect");
            }

        };

        for (int n = 0; n < 10; n++) {
            AtomicReference<String> md5Holder = new AtomicReference<>();
            SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort()) {
                @Override
                public void onMessage(Message message) {
                    md5Holder.set(MD5.create().update(message.asBytes()).asString());
                }
            };
            byte[] data = new byte[126];
            for (int i = 0; i < 100; i++) {
                ThreadLocalRandom.current().nextBytes(data);
                client.send(data);
                client.waitForMessage();
                Assert.assertEquals(MD5.create().update(data).asString(), md5Holder.get());
            }
        }
        Assert.assertEquals(1, counterCreateBuffer.get());
        Assert.assertEquals(0, counterIncreaseBufferSize.get());
    }

    @Test
    public void test_buffers_pool_several_clients_parallel() throws IOException, URISyntaxException, InterruptedException {
        server.setWebsocketFrameLengthLimit(1024 * 1024);
        AtomicInteger counterCreateBuffer = new AtomicInteger();
        AtomicInteger counterIncreaseBufferSize = new AtomicInteger();
        handler = new WebSocketHandler() {
            @Override
            public void onMessage(WebSocketListener listener, Message message) {
                listener.sendMessage(message);
            }

            @Override
            protected Supplier<ByteArrayHolder> createByteArraySupplier() {
                return () -> new ByteArrayHolder() {
                    {
                        counterCreateBuffer.incrementAndGet();
                    }

                    @Override
                    protected void createArray(int minLength) {
                        super.createArray(minLength);
                        counterIncreaseBufferSize.incrementAndGet();
                    }
                };
            }
        };

        for (int n = 0; n < 10; n++) {
            AtomicReference<String> md5Holder = new AtomicReference<>();
            SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:" + getPort()) {
                @Override
                public void onMessage(Message message) {
                    md5Holder.set(MD5.create().update(message.asBytes()).asString());
                }
            };
            Thread thread = new Thread(() -> {
                byte[] data = new byte[126];
                ThreadLocalRandom.current().nextBytes(data);
                try {
                    client.send(data);
                    client.waitForMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Assert.assertEquals(MD5.create().update(data).asString(), md5Holder.get());
            });
            thread.start();
        }
        Thread.sleep(100);
        Assert.assertEquals(1, counterCreateBuffer.get());
        Assert.assertEquals(0, counterIncreaseBufferSize.get());
    }
}
