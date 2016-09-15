package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author: wizzardo
 * Date: 9/3/14
 */
public class EpollOutputStream extends OutputStream {
    private HttpConnection connection;
    private int offset;
    private byte[] buffer;
    protected volatile boolean waiting;

    public EpollOutputStream(HttpConnection connection) {
        this.connection = connection;
        buffer = new byte[16 * 1024];
        offset = 0;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flush();
        waiting = true;
        connection.write(b, off, len, getByteBufferProvider());
        waitFor();
    }

    protected ByteBufferProvider getByteBufferProvider() {
        return (ByteBufferProvider) Thread.currentThread();
    }

    @Override
    public void close() throws IOException {
        flush();
        connection.close();
    }

    @Override
    public void flush() throws IOException {
        if (offset > 0) {
            int length = offset;
            offset = 0;
            write(buffer, 0, length);
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (offset >= buffer.length)
            flush();

        buffer[offset++] = (byte) b;
    }

    protected void waitFor() {
        if (waiting) {
            synchronized (this) {
                while (waiting) {
                    try {
                        this.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    protected void wakeUp() {
        synchronized (this) {
            waiting = false;
            this.notifyAll();
        }
    }
}
