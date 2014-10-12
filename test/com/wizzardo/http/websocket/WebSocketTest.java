package com.wizzardo.http.websocket;

import com.wizzardo.http.ServerTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
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

}
