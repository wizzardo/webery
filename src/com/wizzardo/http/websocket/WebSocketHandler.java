package com.wizzardo.http.websocket;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.ConnectionListener;
import com.wizzardo.http.Handler;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.security.Base64;
import com.wizzardo.tools.security.SHA1;

import java.io.IOException;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public class WebSocketHandler implements Handler {

    @Override
    public Response handle(Request request, Response response) {
        if (request.method() != Request.Method.GET)
            return response.status(Status._405).header(Header.KEY_ALLOW, Request.Method.GET.name());

        if (!Header.VALUE_WEBSOCKET.value.equals(request.header(Header.KEY_UPGRADE)))
            return response.status(Status._400);

        if (!Header.VALUE_UPGRADE.value.equals(request.header(Header.KEY_CONNECTION)))
            return response.status(Status._400);

        if (request.headerLong(Header.KEY_SEC_WEBSOCKET_VERSION, -1) != 13)
            return response.status(Status._400);

        String key = request.header(Header.KEY_SEC_WEBSOCKET_KEY);

        if (key == null || key.isEmpty())
            return response.status(Status._400);

        key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"; // websocket magic
        key = Base64.encodeToString(SHA1.getSHA1(key.getBytes()));

        request.connection().upgrade(new WebSocketListener(request.connection(), this));

        return response.status(Status._101)
                .header(Header.KEY_UPGRADE, Header.VALUE_WEBSOCKET)
                .header(Header.KEY_CONNECTION, Header.VALUE_UPGRADE)
                .header(Header.KEY_SEC_WEBSOCKET_ACCEPT, key);
    }

    public static class WebSocketListener implements ConnectionListener {
        private HttpConnection connection;
        private WebSocketHandler webSocketHandler;
        private Message tempMessage;
        private Frame tempFrame;
        private int read = 0;

        private WebSocketListener(HttpConnection connection, WebSocketHandler webSocketHandler) {
            this.connection = connection;
            this.webSocketHandler = webSocketHandler;
        }

        @Override
        public void onData(HttpConnection connection) {
            try {
                byte[] buffer = connection.getBuffer();
                int r;

                outer:
                while ((r = connection.read(buffer, read, buffer.length - read, (ByteBufferProvider) Thread.currentThread())) > 0) {
                    read += r;
                    while (read > 0) {
                        if (tempFrame == null) {
                            if (!Frame.hasHeaders(buffer, 0, read))
                                continue outer;

                            tempFrame = new Frame();
                        }

                        int k = tempFrame.read(buffer, 0, read);
                        read -= k;
                        if (read != 0)
                            System.arraycopy(buffer, k, buffer, 0, read);

                        if (tempFrame.isComplete()) {
                            if (tempFrame.isMasked())
                                tempFrame.unmask();

                            if (handlePing())
                                continue;

                            if (handleClose())
                                continue;

                            if (tempMessage == null)
                                tempMessage = new Message();

                            tempMessage.add(tempFrame);
                            tempFrame = null;
                        }

                        if (tempMessage != null && tempMessage.isComplete()) {
                            webSocketHandler.onMessage(this, tempMessage);
                            tempMessage = null;
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    connection.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        private boolean handlePing() {
            if (tempFrame.isPing()) {
                tempFrame.setOpcode(Frame.OPCODE_PONG);
                sendFrame(tempFrame);
                tempFrame = null;
                return true;
            }
            return false;
        }

        private boolean handleClose() {
            if (tempFrame.isClose()) {
                connection.setCloseOnFinishWriting(true);
                sendFrame(tempFrame);
                tempFrame = null;
                return true;
            }
            return false;
        }

        @Override
        public void onReady(HttpConnection connection) {
            webSocketHandler.onConnect(this);
        }

        public synchronized void sendMessage(Message message) {
            for (Frame frame : message.getFrames()) {
                connection.write(convertFrameToReadableData(frame), (ByteBufferProvider) Thread.currentThread());
            }
        }

        private synchronized void sendFrame(Frame frame) {
            connection.write(convertFrameToReadableData(frame), (ByteBufferProvider) Thread.currentThread());
        }

        public void close() {
            Frame frame = new Frame();
            frame.setOpcode(Frame.OPCODE_CONNECTION_CLOSE);
            sendFrame(frame);
        }

        private ReadableData convertFrameToReadableData(Frame frame) {
            ReadableBuilder builder = new ReadableBuilder().append(frame.getHeader());

            if (frame.getLength() > 0)
                builder.append(frame.getData(), frame.getOffset(), frame.getLength());

            return builder;
        }
    }

    public void onConnect(WebSocketListener listener) {
    }

    public void onMessage(WebSocketListener listener, Message message) {
    }
}
