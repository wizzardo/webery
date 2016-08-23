package com.wizzardo.http.response;

import com.wizzardo.epoll.readable.ReadableByteBuffer;
import com.wizzardo.epoll.readable.ReadableFile;
import com.wizzardo.http.HttpDateFormatterHolder;
import com.wizzardo.http.framework.ServerConfiguration;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.cache.MemoryLimitedCache;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.security.MD5;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * @author: wizzardo
 * Date: 8/4/14
 */
public class RangeResponseHelper {
    public static final Pattern VERSION_PATTERN = Pattern.compile("\\.v[0-9A-F]{4}");
    protected static final long DEFAULT_CACHE_MEMORY_LIMIT = 32 * 1024 * 1024;
    protected static final long DEFAULT_CACHE_MAX_FILE_SIZE = 5 * 1024 * 1024;
    protected static final long DEFAULT_CACHE_TTL = 5 * 60;
    protected static final boolean DEFAULT_CACHE_GZIP = true;

    protected MemoryLimitedCache<String, FileHolder> filesCache;
    protected final long maxCachedFileSize;

    public RangeResponseHelper() {
        this(DEFAULT_CACHE_MEMORY_LIMIT, DEFAULT_CACHE_TTL, DEFAULT_CACHE_MAX_FILE_SIZE, DEFAULT_CACHE_GZIP);
    }

    public RangeResponseHelper(long cacheMemoryLimit, long cacheTTL, long maxCachedFileSize, boolean gzip) {
        this.maxCachedFileSize = maxCachedFileSize;
        filesCache = createFileHolderCache(cacheMemoryLimit, cacheTTL, gzip);
    }

    public RangeResponseHelper(ServerConfiguration.Resources.Cache cache) {
        if (!cache.enabled) {
            this.maxCachedFileSize = -1;
        } else {
            this.maxCachedFileSize = cache.maxFileSize;
            filesCache = createFileHolderCache(cache.memoryLimit, cache.ttl, cache.gzip);
        }
    }

    protected MemoryLimitedCache<String, FileHolder> createFileHolderCache(long cacheMemoryLimit, long cacheTTL, boolean gzip) {
        return new MemoryLimitedCache<>(cacheMemoryLimit, cacheTTL, path -> {
            byte[] bytes = FileTools.bytes(path);
            if (gzip) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
                GZIPOutputStream gout = new GZIPOutputStream(out);
                gout.write(bytes);
                gout.close();
                bytes = out.toByteArray();
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            String lastModified = HttpDateFormatterHolder.get().format(new Date(new File(path).lastModified()));
            String md5 = MD5.create().update(bytes).asString().toUpperCase();

            return new FileHolder(new ReadableByteBuffer(buffer), md5, lastModified, gzip);
        });
    }

    public static class FileHolder implements MemoryLimitedCache.SizeProvider {
        public final ReadableByteBuffer buffer;
        public final String md5;
        public final String lastModified;
        public final boolean gzip;

        public FileHolder(ReadableByteBuffer buffer, String md5, String lastModified, boolean gzip) {
            this.buffer = buffer;
            this.md5 = md5;
            this.lastModified = lastModified;
            this.gzip = gzip;
        }

        @Override
        public long size() {
            return buffer.length();
        }
    }

    public FileHolder getFileHolder(File file) {
        return file.length() <= maxCachedFileSize ? filesCache.get(file.getAbsolutePath()) : null;
    }

    public Response makeRangeResponse(Request request, Response response, File file) {
        response.appendHeader(Header.KEY_ACCEPT_RANGES, Header.VALUE_BYTES);

        Range range;
        String rangeHeader = request.header(Header.KEY_RANGE);
        FileHolder fileHolder = getFileHolder(file);

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

            if (fileHolder != null) {
                if (fileHolder.md5.equals(request.header(Header.KEY_IF_NONE_MATCH)))
                    return response.status(Status._304);

                if (fileHolder.gzip)
                    response.appendHeader(Header.KV_CONTENT_ENCODING_GZIP);

                response.appendHeader(Header.KEY_ETAG, fileHolder.md5);
                response.appendHeader(Header.KEY_LAST_MODIFIED, fileHolder.lastModified);
                response.appendHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(fileHolder.buffer.length()));
                range = new Range(0, fileHolder.buffer.length() - 1, fileHolder.buffer.length());
            } else {
                response.appendHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(file.length()));
                response.appendHeader(Header.KEY_LAST_MODIFIED, HttpDateFormatterHolder.get().format(new Date(file.lastModified())));
                range = new Range(0, file.length() - 1, file.length());
            }
        }
        response.appendHeader(Header.KEY_CONNECTION, Header.VALUE_KEEP_ALIVE);

        request.connection().getServer().getMimeProvider().provideContentType(response, file);

        try {
            if (fileHolder != null)
                response.setBody(fileHolder.buffer.subBuffer((int) range.from, (int) range.length()));
            else
                response.setBody(new ReadableFile(file, range.from, range.length()));
        } catch (IOException e) {
            throw Unchecked.rethrow(e);
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
