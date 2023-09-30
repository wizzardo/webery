package com.wizzardo.http;

import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ChunkedReadableData extends ReadableBuilder implements BytesConsumer {
    protected final static byte[] RN = "\r\n".getBytes(StandardCharsets.UTF_8);
    protected final BytesProducer producer;
    protected final HttpConnection connection;
    protected volatile boolean last;

    public ChunkedReadableData(BytesProducer producer, HttpConnection connection) throws IOException {
        this.producer = producer;
        this.connection = connection;
        producer.produceTo(this);
    }

    @Override
    public void consume(byte[] bytes, int offset, int length) {
        if (length <= 0)
            last = true;

        int toAppend = Math.max(length, 0);
        append(Integer.toHexString(toAppend).getBytes(StandardCharsets.UTF_8));
        append(RN);
        append(bytes, offset, toAppend);
        append(RN);
    }

    @Override
    public void onComplete() {
        if (last) {
            IOTools.close(connection);
            return;
        }

        try {
            reset();
            producer.produceTo(this);
            connection.send(this);
        } catch (IOException e) {
            throw Unchecked.rethrow(e);
        }
    }
}
