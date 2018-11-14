package com.wizzardo.http.websocket;

import com.wizzardo.epoll.*;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.Buffer;
import com.wizzardo.http.Handler;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.interfaces.Supplier;
import com.wizzardo.tools.misc.pool.Pool;
import com.wizzardo.tools.misc.pool.PoolBuilder;
import com.wizzardo.tools.security.Base64;
import com.wizzardo.tools.security.SHA1;

import java.io.IOException;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public class WebSocketHandler<T extends WebSocketHandler.WebSocketListener> implements Handler {
    protected Pool<ByteArrayHolder> byteArrayHolderPool = new PoolBuilder<ByteArrayHolder>()
            .supplier(createByteArraySupplier())
            .queue(PoolBuilder.createThreadLocalQueueSupplier())
            .build();

    protected Supplier<ByteArrayHolder> createByteArraySupplier() {
        return ByteArrayHolder::new;
    }

    protected static class ByteArrayHolder implements ByteArraySupplier {
        byte[] buffer = new byte[10240];
        boolean used = false;

        @Override
        public byte[] supply(int minLength) {
            if (buffer.length < minLength)
                createArray(minLength);

            return buffer;
        }

        protected void createArray(int minLength) {
            buffer = new byte[minLength];
        }

        public void release(Pool<ByteArraySupplier> pool) {
            if (!used)
                return;

            pool.release(this);
            used = false;
        }
    }

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

        beforeHandshake(request, response);
        if (response.isCommitted())
            return response;

        key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"; // websocket magic
        key = Base64.encodeToString(SHA1.create().update(key.getBytes()).asBytes());

        HttpConnection c = request.connection();
        T listener = createListener(c, this);
        c.onDisconnect(listener);
        c.upgrade(listener);

        response.status(Status._101)
                .header(Header.KEY_UPGRADE, Header.VALUE_WEBSOCKET)
                .header(Header.KEY_CONNECTION, Header.VALUE_UPGRADE)
                .header(Header.KEY_SEC_WEBSOCKET_ACCEPT, key)
                .commit(c);

        c.flush();

        listener.onConnect(c, ByteBufferProvider.current());

        return response;
    }

    protected void beforeHandshake(Request request, Response response) {
    }

    protected T createListener(HttpConnection connection, WebSocketHandler handler) {
        return (T) new WebSocketListener(connection, handler);
    }

    public static class WebSocketListener implements ReadListener<HttpConnection>, ConnectListener<HttpConnection>, DisconnectListener<Connection> {
        protected final HttpConnection connection;
        protected final WebSocketHandler webSocketHandler;
        private Message tempMessage = new Message();
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
        public void onRead(HttpConnection connection, ByteBufferProvider byteBufferProvider) {
            Message message = this.tempMessage;
            try {
                byte[] buffer = Buffer.current().bytes();
                int r;

                outer:
                while ((r = connection.read(buffer, read, buffer.length - read, byteBufferProvider)) > 0) {
                    read += r;
                    while (read > 0) {
                        Frame frame = this.tempFrame;
                        if (frame == null) {
                            if (!Frame.hasHeaders(buffer, 0, read))
                                continue outer;

                            frame = this.tempFrame = new Frame(connection.getServer().getWebsocketFrameLengthLimit());
                            ByteArrayHolder byteArraySupplier = (ByteArrayHolder) webSocketHandler.byteArrayHolderPool.get();
                            byteArraySupplier.used = true;
                            frame.setByteArraySupplier(byteArraySupplier);
                        } else if (frame.getByteArraySupplier() == null) {
                            ByteArrayHolder byteArraySupplier = (ByteArrayHolder) webSocketHandler.byteArrayHolderPool.get();
                            byteArraySupplier.used = true;
                            frame.setByteArraySupplier(byteArraySupplier);
                        }

                        int k = frame.read(buffer, 0, read);
                        if (k == 0)
                            break;

                        read -= k;
                        if (read != 0)
                            System.arraycopy(buffer, k, buffer, 0, read);

                        if (frame.isComplete()) {
                            if (frame.isMasked())
                                frame.unmask();

                            if (handlePing())
                                continue;

                            if (handleClose())
                                continue;

                            message.add(frame);
                            this.tempFrame = null;
                        }

                        if (message.isComplete()) {
                            webSocketHandler.onMessage(this, message);
                            releaseByteBuffers(message);
                            message.clear();
                        }
                    }
                }

            } catch (Exception e) {
                onError(e);
                releaseByteBuffers(message);
            }
        }

        protected void releaseByteBuffers(Message message) {
            for (Frame frame : message.getFrames()) {
                ((ByteArrayHolder) frame.getByteArraySupplier()).release(webSocketHandler.byteArrayHolderPool);
            }
        }

        protected void onError(Exception e) {
            e.printStackTrace();
            connection.close();
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
        public void onConnect(HttpConnection connection, ByteBufferProvider byteBufferProvider) {
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
            if (connection.isAlive()) {
                sendFrame(new Frame(Frame.OPCODE_CONNECTION_CLOSE));
                connection.setCloseOnFinishWriting(true);
            } else
                connection.close();
        }

        public void close(int status, String message) {
            if (connection.isAlive()) {
                sendFrame(Frame.closeFrame(status, message));
                connection.setCloseOnFinishWriting(true);
            } else
                connection.close();
        }

        protected ReadableData convertFrameToReadableData(Frame frame) {
            return new ReadableByteArray(frame.getFrameBytes(), frame.getFrameOffset(), frame.getFrameLength());
        }

        @Override
        public void onDisconnect(Connection connection, ByteBufferProvider bufferProvider) throws IOException {
            releaseByteBuffers(tempMessage);
            webSocketHandler.onDisconnect(this);
        }
    }

    public void onConnect(T listener) {
    }

    public void onDisconnect(T listener) {
    }

    public void onMessage(T listener, Message message) {
    }
}
