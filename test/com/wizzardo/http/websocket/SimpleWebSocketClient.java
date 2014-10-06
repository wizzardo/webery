package com.wizzardo.http.websocket;

import com.wizzardo.tools.misc.BoyerMoore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author: wizzardo
 * Date: 03.10.14
 */
public class SimpleWebSocketClient extends Thread {
    private InputStream in;
    private OutputStream out;
    private byte[] buffer = new byte[1024];
    private byte[] messageBuffer;
    private int bufferOffset = 0;
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

    private WebSocketFrame readFrame() throws IOException {
        boolean readLength = false;
        while (!readLength) {
            bufferOffset += in.read(buffer, bufferOffset, buffer.length - bufferOffset);
            if (bufferOffset > 1) {
                byte l = buffer[1];
                if (l <= 125)
                    readLength = true;
                else if (l == 126 && bufferOffset >= 4)
                    readLength = true;
                else if (l == 127 && bufferOffset >= 10)
                    readLength = true;
            }
        }
        WebSocketFrame frame = new WebSocketFrame(buffer, 0, bufferOffset);
        bufferOffset = 0;
        if (frame.complete)
            return frame;
        else {
            while (!frame.complete) {
                frame.read(in);
            }
            return frame;
        }
    }

    public void onMessage(Message message) {
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

        private static final Random RANDOM = new Random();

        private boolean finalFrame;
        private byte rsv1, rsv2, rsv3;
        private byte opcode;
        private boolean masked;
        private int length;
        private int maskingKey;
        private boolean complete;
        private byte[] data;
        private int offset;
        private int read;

        public WebSocketFrame() {
        }

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
                this.length =
//                        ((long) (bytes[offset + 2] & 0xff) << 56)
//                        + ((long) (bytes[offset + 3] & 0xff) << 48)
//                        + ((long) (bytes[offset + 4] & 0xff) << 40)
//                        + ((long) (bytes[offset + 5] & 0xff) << 32)  // not support long frames
                        +((bytes[offset + 6] & 0xff) << 24)
                                + ((bytes[offset + 7] & 0xff) << 16)
                                + ((bytes[offset + 8] & 0xff) << 8)
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

            complete = length - r >= this.length;

            data = new byte[this.length];
            System.arraycopy(bytes, r, data, 0, length - r);
            this.offset = 0;
            read = length - r;
        }

        @Override
        public String toString() {
            return new String(data, offset, length);
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data, int offset, int length) {
            this.data = data;
            this.offset = offset;
            this.length = length;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }

        public void write(OutputStream out) throws IOException {
            int value = 0;
            value |= FINAL_FRAME;
            value |= OPCODE_TEXT_FRAME;
            out.write(value);

            value = length;
            if (value <= 125) {
                value |= MASKED;
                out.write(value);
            } else if (value < 65536) {
                value |= MASKED;
                out.write(value);
                out.write(length >> 8);
                out.write(length);
            } else {
                value |= MASKED;
                out.write(value);
//                out.write((int) (length >> 56));
//                out.write((int) (length >> 48));
//                out.write((int) (length >> 40));
//                out.write((int) (length >> 32));
                out.write(length >> 24);
                out.write(length >> 16);
                out.write(length);
            }

            byte[] mask = intToBytes(RANDOM.nextInt());
            out.write(mask);
            mask(data, mask, offset, length);
            out.write(data, offset, length);
        }

        private void mask(byte[] data, byte[] mask, int offset, int length) {
            for (int i = offset; i < length + offset; i++) {
                data[i] = (byte) (data[i] ^ mask[(i - offset) % 4]);
            }
        }

        private byte[] intToBytes(int i) {
            byte[] bytes = new byte[4];
            bytes[0] = (byte) ((i >> 24) & 0xff);
            bytes[1] = (byte) ((i >> 16) & 0xff);
            bytes[2] = (byte) ((i >> 8) & 0xff);
            bytes[3] = (byte) (i & 0xff);
            return bytes;
        }

        public void read(InputStream in) throws IOException {
            while (read != length) {
                read += in.read(data, offset + read, (int) (length - read));
            }
            complete = true;
        }
    }

    static class Message {
        private List<WebSocketFrame> frames = new ArrayList<>();

        boolean isComplete() {
            if (frames.isEmpty())
                return false;

            WebSocketFrame frame = frames.get(frames.size() - 1);
            return frame.finalFrame && frame.complete;
        }

        void add(WebSocketFrame frame) {
            frames.add(frame);
        }

        public String asString() {
            return new String(asBytes());
        }

        private byte[] asBytes() {
            int length = 0;
            for (WebSocketFrame frame : frames)
                length += frame.length;

            int offset = 0;
            byte[] data = new byte[length];

            for (WebSocketFrame frame : frames) {
                System.arraycopy(frame.getData(), frame.getOffset(), data, offset, frame.getLength());
                offset += frame.getLength();
            }

            return data;
        }
    }

    public void send(String s) throws IOException {
        send(s.getBytes());
    }

    public void send(byte[] data) throws IOException {
        send(data, 0, data.length);
    }

    public void send(byte[] data, int offset, int length) throws IOException {
        WebSocketFrame frame = new WebSocketFrame();
        frame.setData(data, offset, length);
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
        SimpleWebSocketClient client = new SimpleWebSocketClient("ws://localhost:8080/BrochureDownloader/echo") {
            @Override
            public void onMessage(Message message) {
                System.out.println("onMessage: " + message.asString());
            }
        };
        client.start();

        client.send("foo bar");

    }
}
