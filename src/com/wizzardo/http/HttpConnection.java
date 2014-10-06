package com.wizzardo.http;

import com.wizzardo.epoll.Connection;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.request.RequestReader;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

/**
 * @author: wizzardo
 * Date: 3/14/14
 */
public class HttpConnection extends Connection {
    private volatile byte[] data = new byte[1024];
    private volatile int r = 0;
    private volatile int position = 0;
    private volatile Request request;
    private volatile EpollInputStream inputStream;
    private volatile EpollOutputStream outputStream;
    private volatile State state = State.READING_HEADERS;
    private volatile ConnectionListener listener;
    private boolean ready = false;
    private RequestReader requestReader;

    static enum State {
        READING_HEADERS,
        READING_BODY,
        READING_BODY_MULTIPART,
        WRITING_OUTPUT_STREAM,
        READING_INPUT_STREAM,
        UPGRADED
    }

    public HttpConnection(int fd, int ip, int port) {
        super(fd, ip, port);
    }

    int getBufferSize() {
        return data.length - position;
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

    public void upgrade(ConnectionListener listener) {
        this.listener = listener;
        state = State.UPGRADED;
    }

    boolean processListener() {
        if (listener == null || state != State.UPGRADED)
            return false;

        listener.onData(this);

        return true;
    }

    public boolean hasDataToWrite() {
        return sending != null && !sending.isEmpty();
    }

    private boolean handleHeaders(ByteBuffer bb) {
        if (requestReader == null)
            requestReader = new RequestReader(new LinkedHashMap<>(20));

        int limit, i;
        do {
            limit = readFromByteBuffer(bb);
            i = requestReader.read(data, 0, limit);
            if (i > 0)
                break;
        } while (bb.remaining() > 0);

        if (i < 0)
            return false;

        position = i;
        r = limit;
        request = requestReader.createRequest(this);
        ready = true;
        return checkData(bb);
    }

    private int readFromByteBuffer(ByteBuffer bb) {
        int limit;
        limit = bb.limit();
        limit = Math.min(limit, data.length);
        bb.get(data, 0, limit);
        return limit;
    }

    private boolean checkData(ByteBuffer bb) {
        if (request.contentLength() > 0) {
            if (request.getBody() == null || request.isMultipart()) {
                getInputStream();
                return true;
            }
            ready = request.getBody().read(data, position, r - position);
            state = State.READING_BODY;
            r = 0;
            position = 0;
            return handleData(bb);
        }
        return true;
    }

    private boolean handleData(ByteBuffer bb) {
        int limit;
        while (bb.remaining() > 0) {
            limit = readFromByteBuffer(bb);
            ready = request.getBody().read(data, 0, limit);
        }
        return ready;
    }

    public boolean isRequestReady() {
        return ready;
    }

    public void onFinishingHandling() {
        if (state == State.UPGRADED && listener != null) {
            listener.onReady(this);
            return;
        }

        position = 0;
        ready = false;
        state = State.READING_HEADERS;
        inputStream = null;
        outputStream = null;
        r = 0;
        requestReader = null;
    }

    @Override
    public void onWriteData(ReadableData readable, boolean hasMore) {
        if (state == State.WRITING_OUTPUT_STREAM) {
            outputStream.wakeUp();
            return;
        }

        if (state != State.UPGRADED && !Header.VALUE_CONNECTION_KEEP_ALIVE.value.equalsIgnoreCase(request.header(Header.KEY_CONNECTION.value))) {
            close();
        }
    }

    public RequestReader getRequestReader() {
        return requestReader;
    }

    public Request getRequest() {
        return request;
    }

    public EpollInputStream getInputStream() {
        if (inputStream == null) {
            inputStream = new EpollInputStream(this, data, position, r, request.contentLength());
            state = State.READING_INPUT_STREAM;
        }

        return inputStream;
    }


    public EpollOutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new EpollOutputStream(this);
            state = State.WRITING_OUTPUT_STREAM;
        }

        return outputStream;
    }
}
