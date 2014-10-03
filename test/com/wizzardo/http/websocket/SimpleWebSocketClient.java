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
public class SimpleWebSocketClient {
    private InputStream in;
    private OutputStream out;
    private byte[] buffer = new byte[1024];
    private byte[] messageBuffer;
    private int bufferOffset = 0;
    private int length = 0;
    private int read = 0;

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

    public void waitForMessage() throws IOException {
        if (length == 0) {
            bufferOffset = in.read(buffer);
            WebSocketFrame frame = new WebSocketFrame(buffer, 0, bufferOffset);
            if (frame.complete && frame.finalFrame)
                onMessage(frame.data, frame.offset, frame.length);
            else
                throw new IllegalStateException("not implemented yet");
        }
    }

    public void onMessage(String message) {
    }

    public void onMessage(byte[] bytes, int offset, long length) {
        onMessage(new String(bytes, offset, (int) length));
    }

    static class WebSocketFrame {
        static final int FINAL_FRAME = 1 << 7;
        static final int MASKED = 1 << 7;
        static final int RSV1 = 1 << 6;
        static final int RSV2 = 1 << 5;
        static final int RSV3 = 1 << 4;
        static final int OPCODE = 0x0f;
        static final int OPCODE_CONTINUATION_FRAME = 0;
        static final int OPCODE_TEXT_FRAME = 1;
        static final int OPCODE_CONNECTION_CLOSE = 8;
        static final int OPCODE_PING = 9;
        static final int OPCODE_PONG = 10;
        static final int LENGTH_FIRST_BYTE = 0x7f;

        boolean finalFrame;
        byte rsv1, rsv2, rsv3;
        byte opcode;
        boolean masked;
        long length;
        int maskingKey;
        boolean complete;
        byte[] data;
        int offset;
        int read;

        public WebSocketFrame(byte[] bytes, int offset, int length) {
            byte b = bytes[offset];
            finalFrame = (b & FINAL_FRAME) != 0;
            rsv1 = (byte) (b & RSV1);
            rsv2 = (byte) (b & RSV2);
            rsv3 = (byte) (b & RSV3);

            opcode = (byte) (b & OPCODE);

            b = bytes[offset + 1];
            masked = (b & MASKED) != 0;
            this.length = b & LENGTH_FIRST_BYTE;
            int r = 2;
            if (this.length == 126) {
                this.length = ((bytes[offset + 2] & 0xff) << 8) + (bytes[offset + 3] & 0xff);
                r += 2;
            } else if (this.length == 127) {
                this.length = ((long) (bytes[offset + 2] & 0xff) << 56)
                        + ((long) (bytes[offset + 3] & 0xff) << 48)
                        + ((long) (bytes[offset + 4] & 0xff) << 40)
                        + ((long) (bytes[offset + 5] & 0xff) << 32)
                        + ((long) (bytes[offset + 6] & 0xff) << 24)
                        + ((long) (bytes[offset + 7] & 0xff) << 16)
                        + ((long) (bytes[offset + 8] & 0xff) << 8)
                        + (bytes[offset + 9] & 0xff);
                r += 8;
            }
            if (masked) {
                maskingKey = ((bytes[offset + r] & 0xff) << 24)
                        + ((bytes[offset + r + 1] & 0xff) << 16)
                        + ((bytes[offset + r + 2] & 0xff) << 8)
                        + (bytes[offset + r + 3] & 0xff);
                r += 4;
            }

            complete = length - r == this.length;
            data = bytes;
            this.offset = r;
            read = length - r;
        }

        @Override
        public String toString() {
            return new String(data, offset, (int) length);
        }
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
//        byte[] testMessage = new byte[]{-127, 28, 70, 114, 105, 32, 79, 99, 116, 32, 48, 51, 32, 50, 48, 58, 49, 55, 58, 48, 49, 32, 77, 83, 75, 32, 50, 48, 49, 52};
//        System.out.println(new String(testMessage));
//        System.out.println(testMessage.length);
//        WebSocketFrame message = new WebSocketFrame(testMessage, 0, testMessage.length);
//        System.out.println(message);

        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:8080/BrochureDownloader/test") {
            @Override
            public void onMessage(String message) {
                System.out.println("onMessage: " + message);
            }
        };
        while (true) {
            client.waitForMessage();
        }
    }
}