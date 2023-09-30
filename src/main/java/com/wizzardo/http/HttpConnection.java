package com.wizzardo.http;

import com.wizzardo.epoll.*;
import com.wizzardo.epoll.EpollInputStream;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.request.RequestReader;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: wizzardo
 * Date: 3/14/14
 */
public class HttpConnection<H extends AbstractHttpServer, Q extends Request, S extends Response> extends Connection {

    public static final String HTTP_1_0 = "HTTP/1.0";
    public static final String HTTP_1_1 = "HTTP/1.1";

    protected boolean closeOnFinishWriting = false;
    protected boolean keepAlive = false;
    protected RequestReader requestReader;

    protected Q request = createRequest(createResponse());
    protected H server;

    volatile AtomicReference<Thread> processingBy = new AtomicReference<>();

    public HttpConnection(int fd, int ip, int port, H server) {
        super(fd, ip, port);
        this.server = server;
        sending = createSendingQueue();
        requestReader = server.createRequestReader();
    }

    public void init(int fd, int ip, int port) {
        this.fd = fd;
        this.ip = ip;
        this.port = port;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public Request.State getState() {
        return request.getState();
    }

    public boolean check(Buffer buffer) {
        Request.State state = getState();
        if (state == Request.State.READING_HEADERS) {
            return handleHeaders(buffer);
        } else if (state == Request.State.READING_BODY) {
            return handleData(buffer);
        }
        return false;
    }

    public H getServer() {
        return server;
    }

    public void upgrade(ReadListener<Connection> listener) {
        onRead(listener);
        request.setState(Request.State.UPGRADED);
    }

    protected boolean processInputListener() throws IOException {
        if (readListener == null)
            return false;

        //todo don't need anymore?
        readListener.onRead(this, ByteBufferProvider.current());
        return true;
    }

    protected boolean processOutputListener() throws IOException {
        if (writeListener == null)
            return false;

        writeListener.onWrite(this, ByteBufferProvider.current());
        return true;
    }

    public boolean hasDataToWrite() {
        return sending != null && !sending.isEmpty();
    }

    protected boolean handleHeaders(Buffer buffer) {
        int i = requestReader.read(buffer.bytes(), buffer.position(), buffer.remains());
        if (i == -1 && !requestReader.complete) {
            buffer.clear();
            return false;
        }

        buffer.position(i >= 0 ? i : buffer.limit());
        requestReader.fillRequest(request);
        if (request.method() == Request.Method.HEAD)
            request.response().setHasBody(false);
        keepAlive = prepareKeepAlive();
        request.isReady(true);

        return checkData(buffer); // todo: check that it's needed
    }

    protected boolean prepareKeepAlive() {
        String connection = request.header(Header.KEY_CONNECTION);
        boolean keepAlive;
        if (connection == null) {
            keepAlive = request.protocol().equals(HttpConnection.HTTP_1_1);
        } else {
            keepAlive = !Header.VALUE_CLOSE.value.equalsIgnoreCase(connection);
        }

        if (keepAlive && request.protocol().equals(HttpConnection.HTTP_1_0))
            request.response().appendHeader(Header.KV_CONNECTION_KEEP_ALIVE);

        return keepAlive;
    }

    protected Q createRequest(S response) {
        return (Q) new Request(this, response);
    }

    protected S createResponse() {
        return (S) new Response();
    }

    protected void readFromByteBuffer(ByteBuffer bb, Buffer buffer) {
        int limit = Math.min(bb.remaining(), buffer.free());
        bb.get(buffer.bytes(), buffer.limit(), limit);
        buffer.limit(buffer.limit() + limit);
    }

    protected boolean checkData(Buffer buffer) {
        if (request.contentLength() > 0) {
            if (request.getBody() == null || request.isMultipart()) {
//                getInputStream();
                return true;
            }
            request.setState(Request.State.READING_BODY);
            return handleData(buffer);
        }
        return true;
    }

    protected boolean handleData(Buffer buffer) {
        if (buffer.hasRemaining()) {
            buffer.position(buffer.position() + request.getBody().read(buffer.bytes(), buffer.position(), buffer.remains()));
            if (!buffer.hasRemaining())
                buffer.clear();
        }
        return request.isReady(request.getBody().isReady());
    }

    public boolean isRequestReady() {
        return request.isReady();
    }

    public boolean onFinishingHandling() throws IOException {
        Buffer buffer = Buffer.current();
        if (request.getState() == Request.State.UPGRADED && readListener != null) {
            flush();
            readListener.onRead(this, ByteBufferProvider.current());
            buffer.clear();
            return false;
        }
        if (!keepAlive || request.response().status().code > 300) {
            buffer.clear();
            flush();
            setCloseOnFinishWriting(true);
            return false;
        }

        inputStream = null;
        outputStream = null;
        onRead((ReadListener<Connection>) null);
        onWrite((WriteListener<Connection>) null);
        request.reset();
        requestReader.clear();
        if (buffer.hasRemaining()) {
            handleHeaders(buffer);
        } else {
            buffer.clear();
        }
        return true;
    }

    public void flush() {
        flush(ByteBufferProvider.current());
    }

    public void flush(ByteBufferProvider provider) {
        ByteBufferWrapper buffer = provider.getBuffer();
        try {
            if (buffer.position() == 0) {
                if (!sending.isEmpty())
                    write(provider);
                return;
            }

            int w = write(buffer, 0, buffer.position());

            ByteBuffer bb = buffer.buffer();
            if (w != bb.position()) {
                bb.flip();
                bb.position(w);
                byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes);
                sending.addFirst(new ReadableByteArray(bytes));
            } else {
                if (!sending.isEmpty())
                    write(provider);
            }
        } catch (IOException e) {
            IOTools.close(this);
            throw Unchecked.rethrow(e);
        } finally {
            buffer.clear();
        }
    }

    public void send(ReadableData readableData) {
        sending.add(readableData);
    }

    @Override
    public void onWriteData(ReadableData readable, boolean hasMore) throws IOException {
        if (hasMore)
            return;

        if (processOutputListener())
            return;

        if (!keepAlive && request.getState() != Request.State.UPGRADED && !request.response().isAsync()) {
            IOTools.close(this);
            return;
        }

        if (closeOnFinishWriting)
            IOTools.close(this);
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public Q getRequest() {
        return request;
    }

    public S getResponse() {
        return (S) request.response();
    }

    public EpollInputStream getInputStream() {
        if (inputStream == null) {
            if (request.getBody() != null) {
                byte[] bytes = request.data();
                inputStream = createInputStream(bytes, 0, bytes.length, bytes.length);
            } else {
                Buffer buffer = Buffer.current();
                inputStream = createInputStream(buffer.bytes(), buffer.position(), buffer.limit(), request.contentLength());
                if (buffer.remains() > request.contentLength())
                    buffer.position((int) (buffer.position() + request.contentLength()));
                else {
                    buffer.clear();
                }
            }
            onRead((connection, byteBufferProvider) -> inputStream.wakeUp());
        }

        return inputStream;
    }

    public void flushOutputStream() throws IOException {
        if (outputStream != null)
            outputStream.flush();
    }

    public void setCloseOnFinishWriting(boolean closeOnFinishWriting) {
        this.closeOnFinishWriting = closeOnFinishWriting;
        if (sending == null || sending.isEmpty()) {
            IOTools.close(this);
        }
    }

    @Override
    public void onRead(ByteBufferProvider bufferProvider) throws IOException {
        server.process(this, bufferProvider);
    }

    public void process(ByteBufferProvider bufferProvider) throws IOException {
        if (checkData(bufferProvider)) {
            while (server.processConnection(this)) {
            }
            this.flush();
        }
    }

    protected boolean checkData(ByteBufferProvider bufferProvider) throws IOException {
        if (processInputListener())
            return false;

        //todo: check if response for one request is already in the buffer, but second request started but not ready yet

        ByteBuffer b;
        Buffer buffer = Buffer.current();
        try {
            while ((b = read(buffer.free(), bufferProvider)).limit() > 0) {
                readFromByteBuffer(b, buffer);
                b.clear();
                if (check(buffer))
                    break;
            }
            if (!isRequestReady())
                return false;

        } catch (HttpException e) {
            closeConnection(e.status, bufferProvider);
            e.printStackTrace();
            buffer.clear();
            return false;
        } catch (Exception e) {
            closeConnection(Status._400, bufferProvider);
            buffer.clear();
            e.printStackTrace();
            return false;
        }

        return true;
    }

    protected void closeConnection(Status status, ByteBufferProvider bufferProvider) {
        getResponse()
                .status(status)
                .appendHeader(Header.KV_CONNECTION_CLOSE)
                .commit(this, bufferProvider);
        setCloseOnFinishWriting(true);
    }
}
