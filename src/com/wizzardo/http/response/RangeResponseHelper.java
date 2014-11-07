package com.wizzardo.http.response;

import com.wizzardo.epoll.readable.ReadableFile;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.misc.WrappedException;

import java.io.File;
import java.io.IOException;

/**
 * @author: wizzardo
 * Date: 8/4/14
 */
public class RangeResponseHelper {

    public static Response makeRangeResponse(Request request, Response response, File file) {
        response.setHeader(Header.KEY_ACCEPT_RANGES, Header.VALUE_BYTES);

        Range range;
        String rangeHeader = request.header(Header.KEY_RANGE);
        if (rangeHeader != null) {
            range = new Range(rangeHeader, file.length());
            if (!range.isValid()) {
                response.setStatus(Status._416);
                return response;
            }

            response.setStatus(Status._206);
            response.setHeader(Header.KEY_CONTENT_RANGE, range.toString());
            response.setHeader(Header.KEY_CONTENT_LENGTH, range.length());
        } else {
            range = new Range(0, file.length() - 1, file.length());
            response.setHeader(Header.KEY_CONTENT_LENGTH, file.length());
        }
        response.setHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE);
        try {
            response.setBody(new ReadableFile(file, range.from, range.length()));
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        return response;
    }

    private static class Range {
        long from;
        long to;
        long total;

        private Range(long from, long to, long total) {
            this.from = from;
            this.to = to;
            this.total = total;
        }

        public Range(String range, long length) {
            if (!range.startsWith("bytes="))
                throw new IllegalArgumentException("range string must starts with 'bytes='");

            range = range.substring(6);
            String[] temp = range.split("\\-", 2);
            if (temp[0].length() > 0 && temp[1].length() > 0) {
                from = parseInt(temp[0]);
                to = parseInt(temp[1]);
            } else if (range.startsWith("-")) {
                to = parseInt(temp[1]);
                from = length - to;
                to = length - 1;
            } else {
                from = parseInt(temp[0]);
                to = length - 1;
            }

            if (to > length)
                to = length - 1;

            total = length;
        }

        public boolean isValid() {
            return to >= from && from < total;
        }

        private int parseInt(String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("can't parse " + s + " as integer");
            }
        }

        @Override
        public String toString() {
            return "bytes " + from + "-" + to + "/" + total;
        }

        public long length() {
            return to - from + 1;
        }
    }
}
