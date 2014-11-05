package com.wizzardo.http;

import com.wizzardo.epoll.Connection;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author: wizzardo
 * Date: 8/3/14
 */
public class EpollInputStream extends InputStream {

    protected Connection connection;
    protected int offset;
    protected int limit;
    protected byte[] buffer;
    protected long contentLength = -1;
    protected long read = 0;

    public EpollInputStream(Connection connection, byte[] buffer, int currentOffset, int currentLimit) {
        this(connection, buffer, currentOffset, currentLimit, -1);
    }

    public EpollInputStream(Connection connection, byte[] buffer, int currentOffset, int currentLimit, long contentLength) {
        this.connection = connection;
        this.buffer = buffer;
        offset = currentOffset;
        limit = currentLimit;
        this.contentLength = contentLength;
    }

    public boolean isFinished() {
        return (contentLength > 0 && read >= contentLength) || limit == -1 || !connection.isAlive();
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

        if (isFinished())
            return -1;

        if (available() == 0)
            fillBuffer();

        if (limit < 0)
            return -1;

        if (limit == 0)
            return 0;

        int l = Math.min(len, limit - offset);
        System.arraycopy(buffer, offset, b, off, l);
        offset += l;

        read += l;

        return l;
    }

    @Override
    public int available() throws IOException {
        return limit - offset;
    }

    @Override
    public int read() throws IOException {
        if (offset >= limit)
            fillBuffer();

        int b = buffer[offset] & 0xff;
        offset++;
        return b;
    }

    protected void fillBuffer() throws IOException {
        if (contentLength > 0)
            limit = connection.read(buffer, 0, Math.min(buffer.length, (int) (contentLength - read)));
        else
            limit = connection.read(buffer);
        offset = 0;
        waitForData();
    }

    protected void waitForData() throws IOException {
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

    protected void wakeUp() {
        synchronized (this) {
            notify();
        }
    }
}
