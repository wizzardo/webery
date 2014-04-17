package com.wizzardo.httpserver;

import com.wizzardo.epoll.Connection;
import com.wizzardo.epoll.readable.ReadableBytes;
import com.wizzardo.httpserver.request.HttpHeadersReader;
import com.wizzardo.httpserver.request.RequestHeaders;

import java.nio.ByteBuffer;

/**
 * @author: wizzardo
 * Date: 3/14/14
 */
public class HttpConnection extends Connection {
    private boolean headerReady = false;
    private byte[] data = new byte[1024];
    private volatile int r = 0;
    private volatile int position = 0;
    private volatile ReadableBytes dataToWrite;
    private HttpHeadersReader headersReader;
    private RequestHeaders headers;

    public HttpConnection(int fd, int ip, int port) {
        super(fd, ip, port);
    }

    int getBufferSize() {
        return data.length - position;
    }

    public boolean check(ByteBuffer bb) {
        int limit = bb.limit();
        bb.get(data, 0, limit);
        if (headersReader == null)
            headersReader = new HttpHeadersReader();

        int i = headersReader.read(data, 0, limit);

        if (i < 0)
            return false;
        position = i;
        r = limit;
        headers = headersReader.getHeaders();
        headerReady = true;
        return true;
    }

    public boolean isHttpReady() {
        return headerReady;
    }

    public void reset(String reason) {
        position = 0;
        headerReady = false;
        r = 0;
        headersReader = null;
    }

    void setDataToWrite(ReadableBytes dataToWrite) {
        this.dataToWrite = dataToWrite;
    }

    ReadableBytes getDataToWrite() {
        return dataToWrite;
    }

    public RequestHeaders getHeaders() {
        return headers;
    }
}
