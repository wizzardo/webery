package com.wizzardo.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * @author: wizzardo
 * Date: 9/3/14
 */
public class EpollOutputStream extends OutputStream {
    private HttpConnection connection;
    private int offset;
    private byte[] buffer;
    private volatile CountDownLatch latch;

    public EpollOutputStream(HttpConnection connection) {
        this.connection = connection;
        buffer = new byte[1024];
        offset = 0;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        latch = new CountDownLatch(1);
        connection.write(b, off, len);
        while (latch.getCount() > 0)
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
    }

    @Override
    public void close() throws IOException {
        flush();
        connection.reset();
    }

    @Override
    public void flush() throws IOException {
        if (offset > 0)
            write(buffer, 0, offset);
    }

    @Override
    public void write(int b) throws IOException {
        if (offset >= buffer.length)
            flush();

        offset = 0;
        buffer[offset++] = (byte) b;
    }

    void wakeUp() {
        if (latch != null)
            latch.countDown();
    }
}
