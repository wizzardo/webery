package com.wizzardo.http;

import com.wizzardo.epoll.Connection;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.request.RequestReader;
import com.wizzardo.http.response.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

/**
 * @author: wizzardo
 * Date: 3/14/14
 */
public class HttpConnection<H extends AbstractHttpServer, Q extends Request, S extends Response, I extends EpollInputStream, O extends EpollOutputStream> extends Connection {

    public static final String HTTP_1_0 = "HTTP/1.0";
    public static final String HTTP_1_1 = "HTTP/1.1";

    private volatile byte[] buffer = new byte[1024];
    private volatile int r = 0;
    private volatile int position = 0;
    private I inputStream;
    private O outputStream;
    private volatile State state = State.READING_HEADERS;
    private volatile InputListener<HttpConnection> inputListener;
    private volatile OutputListener<HttpConnection> outputListener;
    private volatile boolean closeOnFinishWriting = false;
    private boolean ready = false;
    private boolean keepAlive = false;
    private RequestReader requestReader;
    protected S response;
    protected Q request;
    protected H server;

    static enum State {
        READING_HEADERS,
        READING_BODY,
        UPGRADED
    }

    public HttpConnection(int fd, int ip, int port, H server) {
        super(fd, ip, port);
        this.server = server;
    }

    int getBufferSize() {
        return buffer.length - position;
    }

    public State getState() {
        return state;
    }

    public boolean check(ByteBuffer bb) {
        switch (state) {
            case READING_HEADERS:
                return handleHeaders(bb);

            case READING_BODY:
                return handleData(bb);
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

    boolean processInputListener() {
        if (inputListener == null)
            return false;

        inputListener.onReadyToRead(this);
        return true;
    }

    boolean processOutputListener() {
        if (outputListener == null)
            return false;

        outputListener.onReadyToWrite(this);
        return true;
    }

    public boolean hasDataToWrite() {
        return sending != null && !sending.isEmpty();
    }

    private boolean handleHeaders(ByteBuffer bb) {
        RequestReader requestReader = this.requestReader;
        if (requestReader == null) {
            requestReader = new RequestReader(new LinkedHashMap<>(20));
            this.requestReader = requestReader;
        }

        int limit, i;
        byte[] buffer = this.buffer;
        do {
            limit = readFromByteBuffer(bb);
            i = requestReader.read(buffer, 0, limit);
            if (i > 0)
                break;
        } while (bb.hasRemaining());

        if (i < 0)
            return false;

        this.requestReader = null;
        position = i;
        r = limit;
        request = createRequest();
        response = createResponse();
        requestReader.fillRequest(request);
        if (request.method() == Request.Method.HEAD)
            response.setHasBody(false);
        keepAlive = prepareKeepAlive();
        ready = true;
        return checkData(bb);
    }

    protected boolean prepareKeepAlive() {
        String connection = request.header(Header.KEY_CONNECTION);
        boolean keepAlive = (request.protocol().equals(HttpConnection.HTTP_1_1) && connection == null) || Header.VALUE_CONNECTION_KEEP_ALIVE.value.equalsIgnoreCase(connection);
        if (keepAlive && request.protocol().equals(HttpConnection.HTTP_1_0))
            response.setHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE);

        return keepAlive;
    }

    protected Q createRequest() {
        return (Q) new Request(this);
    }

    protected S createResponse() {
        return (S) new Response();
    }

    private int readFromByteBuffer(ByteBuffer bb) {
        int limit;
        limit = bb.remaining();
        limit = Math.min(limit, buffer.length);
        bb.get(buffer, 0, limit);
        return limit;
    }

    private boolean checkData(ByteBuffer bb) {
        if (request.contentLength() > 0) {
            if (request.getBody() == null || request.isMultipart()) {
                getInputStream();
                return true;
            }
            ready = request.getBody().read(buffer, position, r - position);
            state = State.READING_BODY;
            r = 0;
            position = 0;
            return handleData(bb);
        }
        return true;
    }

    private boolean handleData(ByteBuffer bb) {
        int limit;
        while (bb.hasRemaining()) {
            limit = readFromByteBuffer(bb);
            ready = request.getBody().read(buffer, 0, limit);
        }
        return ready;
    }

    public boolean isRequestReady() {
        return ready;
    }

    public void onFinishingHandling() {
        if (state == State.UPGRADED && inputListener != null) {
            inputListener.onReady(this);
            return;
        }

        position = 0;
        ready = false;
        state = State.READING_HEADERS;
        inputStream = null;
        outputStream = null;
        inputListener = null;
        outputListener = null;
        r = 0;
    }

    @Override
    public void onWriteData(ReadableData readable, boolean hasMore) {
        if (hasMore)
            return;

        if (processOutputListener())
            return;

        if (state != State.UPGRADED && !keepAlive) {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (closeOnFinishWriting)
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            } else
                inputStream = createInputStream(buffer, position, r, request.contentLength());
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

    public byte[] getBuffer() {
        return buffer;
    }

    public void setCloseOnFinishWriting(boolean closeOnFinishWriting) {
        this.closeOnFinishWriting = closeOnFinishWriting;
    }
}
