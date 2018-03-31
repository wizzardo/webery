package com.wizzardo.http;

import com.wizzardo.epoll.Connection;
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

    //    private volatile byte[] buffer = new byte[1024];
//    private volatile int r = 0;
//    private volatile int position = 0;
    private I inputStream;
    private O outputStream;
    private volatile State state = State.READING_HEADERS;
    private volatile InputListener<HttpConnection> inputListener;
    private volatile OutputListener<HttpConnection> outputListener;
    private volatile boolean closeOnFinishWriting = false;
    private boolean ready = false;
    private boolean keepAlive = false;
    private RequestReader requestReader = new RequestReader(new LinkedHashMap<>(16));
    protected S response = createResponse();
    protected Q request = createRequest();
    protected H server;

    volatile AtomicReference<Thread> processingBy = new AtomicReference<>();

    static enum State {
        READING_HEADERS,
        READING_BODY,
        UPGRADED
    }

    public HttpConnection(int fd, int ip, int port, H server) {
        super(fd, ip, port);
        this.server = server;
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

    public State getState() {
        return state;
    }

    public boolean check(Buffer buffer) {
        switch (state) {
            case READING_HEADERS:
                return handleHeaders(buffer);

            case READING_BODY:
                return handleData(buffer);
        }
        return false;
    }

    public H getServer() {
        return server;
    }

    public void upgrade(InputListener<HttpConnection> listener) {
        setInputListener(listener);
        state = State.UPGRADED;
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
        request.reset();
        response.reset();
        requestReader.fillRequest(request);
        if (request.method() == Request.Method.HEAD)
            response.setHasBody(false);
        keepAlive = prepareKeepAlive();
        ready = true;
        return checkData(buffer);
    }

    protected boolean prepareKeepAlive() {
        String connection = request.header(Header.KEY_CONNECTION);
        boolean keepAlive = (request.protocol().equals(HttpConnection.HTTP_1_1) && connection == null) || Header.VALUE_KEEP_ALIVE.value.equalsIgnoreCase(connection);
        if (keepAlive && request.protocol().equals(HttpConnection.HTTP_1_0))
            response.appendHeader(Header.KV_CONNECTION_KEEP_ALIVE);

        return keepAlive;
    }

    protected Q createRequest() {
        return (Q) new Request(this);
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
            state = State.READING_BODY;
            return ready = request.getBody().isReady();
        }
        return true;
    }

    protected boolean handleData(Buffer buffer) {
        if (buffer.hasRemaining()) {
            request.getBody().read(buffer.bytes(), buffer.position(), buffer.remains());
            ready = request.getBody().isReady();
        }
        return ready;
    }

    public boolean isRequestReady() {
        return ready;
    }

    public boolean onFinishingHandling() {
        if (state == State.UPGRADED && inputListener != null) {
            inputListener.onReady(this);
            return false;
        }
        Buffer buffer = Buffer.current();
        if (!keepAlive || response.status().code > 300) {
            buffer.clear();
            setCloseOnFinishWriting(true);
            return false;
        }

        ready = false;
        inputStream = null;
        outputStream = null;
        inputListener = null;
        outputListener = null;
        requestReader.clear();
        state = State.READING_HEADERS;
        if (buffer.hasRemaining()) {
            handleHeaders(buffer);
        } else {
            buffer.clear();
        }
        return true;
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

        if (!keepAlive && state != State.UPGRADED && !response.isAsync()) {
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
        return response;
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
