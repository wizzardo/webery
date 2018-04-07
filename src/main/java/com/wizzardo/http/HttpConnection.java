package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.epoll.Connection;
import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.request.RequestReader;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.io.IOTools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: wizzardo
 * Date: 3/14/14
 */
public class HttpConnection<H extends AbstractHttpServer, Q extends Request, S extends Response, I extends EpollInputStream, O extends EpollOutputStream> extends Connection {

    public static final String HTTP_1_0 = "HTTP/1.0";
    public static final String HTTP_1_1 = "HTTP/1.1";

    private I inputStream;
    private O outputStream;
    private volatile InputListener<HttpConnection> inputListener;
    private volatile OutputListener<HttpConnection> outputListener;
    private boolean closeOnFinishWriting = false;
    private boolean keepAlive = false;
    private RequestReader requestReader;

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

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            IOTools.close(inputListener);
        }
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

    public void upgrade(InputListener<HttpConnection> listener) {
        setInputListener(listener);
        request.setState(Request.State.UPGRADED);
    }

    public void setInputListener(InputListener<HttpConnection> listener) {
        if (this.inputListener != null && listener != null)
            throw new IllegalStateException("InputListener already was set");
        this.inputListener = listener;
    }

    public void setOutputListener(OutputListener<HttpConnection> listener) {
        if (this.outputListener != null && listener != null)
            throw new IllegalStateException("OutputListener already was set");
        this.outputListener = listener;
    }

    protected boolean processInputListener() {
        if (inputListener == null)
            return false;

        inputListener.onReadyToRead(this);
        return true;
    }

    protected boolean processOutputListener() {
        if (outputListener == null)
            return false;

        outputListener.onReadyToWrite(this);
        return true;
    }

    public boolean hasDataToWrite() {
        return sending != null && !sending.isEmpty();
    }

    protected boolean handleHeaders(Buffer buffer) {
        int i = requestReader.read(buffer.bytes(), buffer.position(), buffer.remains());
        if (i == -1 && !requestReader.complete)
            return false;

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
        boolean keepAlive = (request.protocol().equals(HttpConnection.HTTP_1_1) && connection == null) || Header.VALUE_KEEP_ALIVE.value.equalsIgnoreCase(connection);
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
        int limit = Math.min(bb.remaining(), buffer.capacity());
        bb.get(buffer.bytes(), 0, limit);
        buffer.position(0);
        buffer.limit(limit);
    }

    protected boolean checkData(Buffer buffer) {
        if (request.contentLength() > 0) {
            if (request.getBody() == null || request.isMultipart()) {
//                getInputStream();
                return true;
            }
            request.getBody().read(buffer.bytes(), buffer.position(), buffer.remains());
            request.setState(Request.State.READING_BODY);
            return request.isReady(request.getBody().isReady());
        }
        return true;
    }

    protected boolean handleData(Buffer buffer) {
        if (buffer.hasRemaining()) {
            request.getBody().read(buffer.bytes(), buffer.position(), buffer.remains());
        }
        return request.isReady(request.getBody().isReady());
    }

    public boolean isRequestReady() {
        return request.isReady();
    }

    public boolean onFinishingHandling() {
        if (request.getState() == Request.State.UPGRADED && inputListener != null) {
            inputListener.onReady(this);
            return false;
        }
        Buffer buffer = Buffer.current();
        if (!keepAlive || request.response().status().code > 300) {
            buffer.clear();
            flush();
            setCloseOnFinishWriting(true);
            return false;
        }

        inputStream = null;
        outputStream = null;
        inputListener = null;
        outputListener = null;
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
        ByteBufferProvider provider = ByteBufferProvider.current();
        ByteBufferWrapper buffer = provider.getBuffer();
        if (buffer.position() == 0) {
            if (!sending.isEmpty())
                write(provider);
            return;
        }

        try {
            int w = write(buffer, 0, buffer.position());

            ByteBuffer bb = buffer.buffer();
            if (w != bb.position()) {
                bb.flip();
                bb.position(w);
                byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes);
                sending.addFirst(new ReadableByteArray(bytes));
                buffer.clear();
            } else {
                buffer.clear();
                if (!sending.isEmpty())
                    write(provider);
            }
        } catch (IOException e) {
            IOTools.close(this);
        }
    }

    public void send(ReadableData readableData) {
        ReadableData peek = sending.peek();
        if (peek != null) {
            ((ReadableBuilder) peek).append(readableData);
        } else
            sending.add(readableData);
    }

    public InputListener<HttpConnection> getInputListener() {
        return inputListener;
    }

    @Override
    public void onWriteData(ReadableData readable, boolean hasMore) {
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

    public I getInputStream() {
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
            setInputListener(connection -> inputStream.wakeUp());
        }

        return inputStream;
    }

    public void flushOutputStream() throws IOException {
        if (outputStream != null)
            outputStream.flush();
    }

    public O getOutputStream() {
        if (outputStream == null) {
            outputStream = createOutputStream();
            setOutputListener(connection -> outputStream.wakeUp());
        }

        return outputStream;
    }

    protected I createInputStream(byte[] buffer, int currentOffset, int currentLimit, long contentLength) {
        return (I) new EpollInputStream(this, buffer, currentOffset, currentLimit, contentLength);
    }

    protected O createOutputStream() {
        return (O) new EpollOutputStream(this);
    }

    public void setCloseOnFinishWriting(boolean closeOnFinishWriting) {
        this.closeOnFinishWriting = closeOnFinishWriting;
        if (sending == null || sending.isEmpty()) {
            IOTools.close(this);
        }
    }
}
