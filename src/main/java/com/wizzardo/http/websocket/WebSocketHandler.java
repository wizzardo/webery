package com.wizzardo.http.websocket;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.Handler;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.InputListener;
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
public class WebSocketHandler<T extends WebSocketHandler.WebSocketListener> implements Handler {

    @Override
    public Response handle(Request request, Response response) {
        if (request.method() != Request.Method.GET)
            return response.status(Status._405).header(Header.KEY_ALLOW, Request.Method.GET.name());

        if (!Header.VALUE_WEBSOCKET.value.equals(request.header(Header.KEY_UPGRADE)))
            return response.status(Status._400);

        String connection = request.header(Header.KEY_CONNECTION);
        if (!Header.VALUE_UPGRADE.value.equals(connection) && !"keep-alive, Upgrade".equalsIgnoreCase(connection))
            return response.status(Status._400);

        if (request.headerLong(Header.KEY_SEC_WEBSOCKET_VERSION, -1) != 13)
            return response.status(Status._400);

        String key = request.header(Header.KEY_SEC_WEBSOCKET_KEY);

        if (key == null || key.isEmpty())
            return response.status(Status._400);

        key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"; // websocket magic
        key = Base64.encodeToString(SHA1.create().update(key.getBytes()).asBytes());

        request.connection().upgrade(createListener(request.connection(), this));

        return response.status(Status._101)
                .header(Header.KEY_UPGRADE, Header.VALUE_WEBSOCKET)
                .header(Header.KEY_CONNECTION, Header.VALUE_UPGRADE)
                .header(Header.KEY_SEC_WEBSOCKET_ACCEPT, key);
    }

    protected T createListener(HttpConnection connection, WebSocketHandler handler) {
        return (T) new WebSocketListener(connection, handler);
    }

    public static class WebSocketListener implements InputListener<HttpConnection> {
        protected final HttpConnection connection;
        protected final WebSocketHandler webSocketHandler;
        private Message tempMessage;
        private Frame tempFrame;
        private int read = 0;

        public WebSocketListener(HttpConnection connection, WebSocketHandler webSocketHandler) {
            this.connection = connection;
            this.webSocketHandler = webSocketHandler;
        }

        public Request getRequest() {
            return connection.getRequest();
        }

        @Override
        public void onReadyToRead(HttpConnection connection) {
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

                            tempFrame = new Frame(connection.getServer().getWebsocketFrameLengthLimit());
                        }

                        int k = tempFrame.read(buffer, 0, read);
                        if (k == 0)
                            break;

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

            } catch (Exception e) {
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
                sendFrame(tempFrame);
                connection.setCloseOnFinishWriting(true);
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

        protected synchronized void sendFrame(Frame frame) {
            connection.write(convertFrameToReadableData(frame), (ByteBufferProvider) Thread.currentThread());
        }

        public void close() {
            if (connection.isAlive())
                sendFrame(new Frame(Frame.OPCODE_CONNECTION_CLOSE));

            webSocketHandler.onDisconnect(this);
        }

        protected ReadableData convertFrameToReadableData(Frame frame) {
            return new ReadableByteArray(frame.getFrameBytes(), frame.getFrameOffset(), frame.getFrameLength());
        }
    }

    public void onConnect(T listener) {
    }

    public void onDisconnect(T listener) {
    }

    public void onMessage(T listener, Message message) {
    }
}
