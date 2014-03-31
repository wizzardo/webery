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
    boolean headerReady = false;
    byte[] data = new byte[1024];
    volatile int r = 0;
    volatile int position = 0;
    volatile ReadableBytes writeData;
    HttpHeadersReader headersReader;
    RequestHeaders headers;

    private static final byte[] EMPTY = new byte[0];

    public HttpConnection(int fd, int ip, int port) {
        super(fd, ip, port);
    }

    int getBufferSize() {
        return 1024;
    }

    public boolean check(ByteBuffer bb) {
        if (bb.limit() > data.length - r) {
            byte[] b = new byte[(int) (bb.limit() * 1.5)];
            try {
                System.arraycopy(data, 0, b, 0, r);
            } catch (Exception e) {
                e.printStackTrace();
            }
            data = b;
        }
        int limit = bb.limit();
        bb.get(data, r, limit);
        if (headersReader == null)
            headersReader = new HttpHeadersReader();

        int i = headersReader.read(data, r, limit);
        r += limit;

        if (i < 0)
            return false;
        position = i;
        headers = headersReader.getHeaders();
        return true;
    }

    public boolean isHttpReady() {
        return headerReady;
    }

    public int getHeaderLength() {
        return position - 4;
    }

    public void reset(String reason) {
        position = 0;
        headerReady = false;
        r = 0;
        headersReader = null;
    }

}
