package com.wizzardo.http.response;

import com.wizzardo.epoll.readable.ReadableByteBuffer;
import com.wizzardo.epoll.readable.ReadableFile;
import com.wizzardo.http.HttpDateFormatterHolder;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.cache.MemoryLimitedCache;
import com.wizzardo.tools.misc.UncheckedThrow;
import com.wizzardo.tools.security.MD5;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

/**
 * @author: wizzardo
 * Date: 8/4/14
 */
public class RangeResponseHelper {
    public static final long CACHE_MEMORY_LIMIT = 1024 * 1024 * 1024;
    public static final long CACHE_FILE_LENGTH_LIMIT = 10 * 1024 * 1024;
    public static final long CACHE_TTL = 5 * 60;

    public static MemoryLimitedCache<String, FileHolder> filesCache = new MemoryLimitedCache<>(CACHE_MEMORY_LIMIT, CACHE_TTL, s -> {
        try {
            RandomAccessFile aFile = new RandomAccessFile(s, "r");
            FileChannel inChannel = aFile.getChannel();
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) inChannel.size());
            inChannel.read(buffer);
            return new FileHolder(new ReadableByteBuffer(buffer), MD5.getMD5AsString(s));
        } catch (IOException e) {
            throw UncheckedThrow.rethrow(e);
        }
    });

    public static class FileHolder implements MemoryLimitedCache.SizeProvider {
        public final ReadableByteBuffer buffer;
        public final String md5;

        public FileHolder(ReadableByteBuffer buffer, String md5) {
            this.buffer = buffer;
            this.md5 = md5;
        }

        @Override
        public long size() {
            return buffer.length();
        }
    }

    public static Response makeRangeResponse(Request request, Response response, File file) {
        response.appendHeader(Header.KEY_ACCEPT_RANGES, Header.VALUE_BYTES);

        Range range;
        String rangeHeader = request.header(Header.KEY_RANGE);
        FileHolder fileHolder = null;

        if (file.length() <= CACHE_FILE_LENGTH_LIMIT)
            fileHolder = filesCache.get(file.getAbsolutePath());

        if (rangeHeader != null) {
            range = new Range(rangeHeader, file.length());
            if (!range.isValid()) {
                response.setStatus(Status._416);
                return response;
            }

            response.setStatus(Status._206);
            response.appendHeader(Header.KEY_CONTENT_RANGE, range.toString());
            response.appendHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(range.length()));
        } else {
            Date modifiedSince = request.headerDate(Header.KEY_IF_MODIFIED_SINCE);
            if (modifiedSince != null && modifiedSince.getTime() >= file.lastModified())
                return response.status(Status._304);

            if (fileHolder != null && fileHolder.md5.equals(request.header(Header.KEY_IF_NONE_MATCH)))
                return response.status(Status._304);

            range = new Range(0, file.length() - 1, file.length());
            response.appendHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(file.length()));
            if (fileHolder != null)
                response.appendHeader(Header.KEY_ETAG, fileHolder.md5);
            response.appendHeader(Header.KEY_LAST_MODIFIED, HttpDateFormatterHolder.get().format(new Date(file.lastModified())));
        }
        response.appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE);
        try {
            if (fileHolder != null)
                response.setBody(fileHolder.buffer.subBuffer((int) range.from, (int) range.length()));
            else
                response.setBody(new ReadableFile(file, range.from, range.length()));
        } catch (IOException e) {
            throw UncheckedThrow.rethrow(e);
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
                from = parseLong(temp[0]);
                to = parseLong(temp[1]);
            } else if (range.startsWith("-")) {
                to = parseLong(temp[1]);
                from = length - to;
                to = length - 1;
            } else {
                from = parseLong(temp[0]);
                to = length - 1;
            }

            if (to > length)
                to = length - 1;

            total = length;
        }

        public boolean isValid() {
            return to >= from && from < total;
        }

        private long parseLong(String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("can't parse " + s + " as long");
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
