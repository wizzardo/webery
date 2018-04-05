package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferWrapper;

import java.util.Date;

/**
 * Created by wizzardo on 28.03.15.
 */
public class ServerDate {

    protected volatile DateHolder holder = new DateHolder();

    public String getDateAsString() {
        return getValidHolder().text;
    }

    public byte[] getDateAsBytes() {
        return getValidHolder().bytes;
    }

    public ReadableDirectByteBuffer getDateAsBuffer() {
        return getValidHolder().buffer;
    }

    private DateHolder getValidHolder() {
        DateHolder dateHolder = holder;
        long time = System.currentTimeMillis();
        if (time >= dateHolder.validUntil) {
            do {
                dateHolder = dateHolder.next;
                if (dateHolder == null)
                    dateHolder = new DateHolder(time);
            } while (time >= dateHolder.validUntil);

            holder = dateHolder;
        }
        return dateHolder;
    }

    private static class DateHolder {
        final String text;
        final byte[] bytes;
        final ReadableDirectByteBuffer buffer;
        final long validUntil;
        final DateHolder next;

        public DateHolder(long time) {
            this(time, 0);
        }

        public DateHolder(long time, int counter) {
            text = "Date: " + HttpDateFormatterHolder.get().format(new Date(time)) + "\r\n";
            bytes = text.getBytes();
            buffer = new ReadableDirectByteBuffer(new ByteBufferWrapper(bytes));
            validUntil = (time / 1000 + 1) * 1000;
            if (counter < 100)
                next = new DateHolder(time + 1000, counter + 1);
            else
                next = null;
        }

        public DateHolder() {
            this(System.currentTimeMillis());
        }
    }
}
