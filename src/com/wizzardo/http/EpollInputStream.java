package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author: wizzardo
 * Date: 8/3/14
 */
class EpollInputStream extends InputStream {

    private Connection connection;
    private int offset;
    private int limit;
    private byte[] buffer;
    private int contentLength = -1;
    private int read = 0;

    public EpollInputStream(Connection connection, byte[] buffer, int currentOffset, int currentLimit) {
        this(connection, buffer, currentOffset, currentLimit, -1);
    }

    public EpollInputStream(Connection connection, byte[] buffer, int currentOffset, int currentLimit, int contentLength) {
        this.connection = connection;
        this.buffer = buffer;
        offset = currentOffset;
        limit = currentLimit;
        this.contentLength = contentLength;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if ((contentLength > 0 && read >= contentLength) || limit == -1 || !connection.isAlive())
            return -1;

        if (offset >= limit)
            fillBuffer();

        if (limit < 0)
            return -1;

        int l = Math.min(len, limit - offset);
        System.arraycopy(buffer, offset, b, off, l);
        offset += l;

        read += l;

        return l;
    }

    @Override
    public int read() throws IOException {
        if (offset >= limit)
            fillBuffer();

        int b = buffer[offset] & 0xff;
        offset++;
        return b;
    }

    private void fillBuffer() throws IOException {
        limit = connection.read(buffer);
        offset = 0;
        if (limit == 0) {
            synchronized (this) {
                while ((limit = connection.read(buffer)) == 0) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    void wakeUp() {
        synchronized (this) {
            notify();
        }
    }
}
