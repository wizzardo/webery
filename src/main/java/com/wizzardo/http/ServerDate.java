package com.wizzardo.http;

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

    private DateHolder getValidHolder() {
        DateHolder dateHolder = holder;
        long time = System.currentTimeMillis();
        if (time >= dateHolder.validUntil) {
            dateHolder = new DateHolder(time);
            holder = dateHolder;
        }
        return dateHolder;
    }

    private static class DateHolder {
        final String text;
        final byte[] bytes;
        final long validUntil;

        public DateHolder(long time) {
            text = "Date: " + HttpDateFormatterHolder.get().format(new Date(time)) + "\r\n";
            bytes = text.getBytes();
            validUntil = (time / 1000 + 1) * 1000;
        }

        public DateHolder() {
            this(System.currentTimeMillis());
        }
    }
}
