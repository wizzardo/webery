package com.wizzardo.http.websocket;

import com.wizzardo.tools.misc.BoyerMoore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author: wizzardo
 * Date: 03.10.14
 */
public class SimpleWebSocketClient extends Thread {
    private InputStream in;
    private OutputStream out;
    private byte[] buffer = new byte[1024];
    private int bufferOffset = 0;

    public SimpleWebSocketClient(String url) throws URISyntaxException, IOException {
        URI u = new URI(url.trim());
        if (u.getScheme().equals("wss"))
            throw new IllegalArgumentException("wss not implemented yet");

        if (!u.getScheme().equals("ws"))
            throw new IllegalArgumentException("url must use ws scheme");

        handShake(u);
    }

    private void handShake(URI uri) throws IOException {
        Socket s = new Socket(uri.getHost(), uri.getPort());
        String request = "GET " + uri.getRawPath() + " HTTP/1.1\r\n" +
                "Host: " + uri.getHost() + (uri.getPort() != 80 ? ":" + uri.getPort() : "") + "\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==\r\n" +
//                "Sec-WebSocket-Protocol: chat, superchat\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "Origin: http://" + uri.getHost() + (uri.getPort() != 80 ? ":" + uri.getPort() : "") + "\r\n\r\n";
        System.out.println(request);

        in = s.getInputStream();
        out = s.getOutputStream();

        out.write(request.getBytes());
        out.flush();

        BoyerMoore boyerMoore = new BoyerMoore("\r\n\r\n");
        int response = 0;
        while ((bufferOffset += in.read(buffer, bufferOffset, buffer.length - bufferOffset)) != -1) {
//            System.out.println(new String(bytes, 0, r));
            if ((response = boyerMoore.search(buffer, 0, bufferOffset)) >= 0)
                break;
        }

        System.out.println(new String(buffer, 0, response));
        bufferOffset = 0;
    }

    @Override
    public void run() {
        while (true) {
            try {
                waitForMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitForMessage() throws IOException {
        Message message = new Message();
        while (!message.isComplete()) {
            message.add(readFrame());
        }
        onMessage(message);
    }

    private Frame readFrame() throws IOException {
        while (!Frame.hasHeaders(buffer, 0, bufferOffset)) {
            bufferOffset += in.read(buffer, bufferOffset, buffer.length - bufferOffset);
        }
        Frame frame = new Frame();
        int r = frame.read(buffer, 0, bufferOffset);
        bufferOffset -= r;
        if (bufferOffset != 0)
            System.arraycopy(buffer, r, buffer, 0, bufferOffset);

        if (frame.isComplete())
            return frame;
        else {
            while (!frame.isComplete()) {
                frame.read(in);
            }
            return frame;
        }
    }

    public void onMessage(Message message) {
    }

    public void send(Message message) throws IOException {
        for (Frame frame : message.getFrames()) {
            frame.mask();
            frame.write(out);
        }
    }

    public void send(String s) throws IOException {
        send(s.getBytes());
    }

    public void send(byte[] data) throws IOException {
        send(data, 0, data.length);
    }

    public void send(byte[] data, int offset, int length) throws IOException {
        Frame frame = new Frame();
        frame.setData(data, offset, length);
        frame.mask();
        frame.write(out);
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
//        byte[] testMessage = new byte[]{-127, 28, 70, 114, 105, 32, 79, 99, 116, 32, 48, 51, 32, 50, 48, 58, 49, 55, 58, 48, 49, 32, 77, 83, 75, 32, 50, 48, 49, 52};
//        System.out.println(new String(testMessage));
//        System.out.println(testMessage.length);
//        WebSocketFrame message = new WebSocketFrame(testMessage, 0, testMessage.length);
//        System.out.println(message);

//        String s = "WebSocketFrame message = new WebSocketFrame";
//        byte[] b = s.getBytes();
//        WebSocketFrame frame = new WebSocketFrame();
//        frame.mask(b, frame.intToBytes(123123), 0, b.length);
//        System.out.println(new String(b));
//        frame.mask(b, frame.intToBytes(123123), 0, b.length);
//        System.out.println(new String(b));


//        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:8080/BrochureDownloader/test") {
//        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:8080/BrochureDownloader/echo") {
        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:8084/echo") {
            //        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:8084/time") {
            @Override
            public void onMessage(Message message) {
                System.out.println("onMessage: " + message.asString());
            }
        };
        client.start();

        client.send("foo bar");

    }
}
