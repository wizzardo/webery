package com.wizzardo.http.response;

import com.wizzardo.epoll.readable.ReadableByteBuffer;
import com.wizzardo.epoll.readable.ReadableFile;
import com.wizzardo.http.HttpDateFormatterHolder;
import com.wizzardo.http.framework.ServerConfiguration;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.cache.MemoryLimitedCacheWrapper;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.security.MD5;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.zip.GZIPOutputStream;

/**
 * @author: wizzardo
 * Date: 8/4/14
 */
public class RangeResponseHelper {
    protected static final long DEFAULT_CACHE_MEMORY_LIMIT = 32 * 1024 * 1024;
    protected static final long DEFAULT_CACHE_MAX_FILE_SIZE = 5 * 1024 * 1024;
    protected static final long DEFAULT_CACHE_TTL = 5 * 60;
    protected static final boolean DEFAULT_CACHE_GZIP = true;
    protected static final String MAX_AGE_1_YEAR = "max-age=31556926";

    protected MemoryLimitedCacheWrapper<String, FileHolder> filesCache;
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

    protected MemoryLimitedCacheWrapper<String, FileHolder> createFileHolderCache(long cacheMemoryLimit, long cacheTTL, boolean gzip) {
        return new MemoryLimitedCacheWrapper<>(new Cache<>("resources", cacheTTL, path -> createFileHolder(path, gzip)), cacheMemoryLimit);
    }

    protected FileHolder createFileHolder(String path, boolean gzip) throws IOException {
        byte[] bytes = FileTools.bytes(path);
        ReadableByteBuffer gzipped = null;
        if (gzip) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
            GZIPOutputStream gout = new GZIPOutputStream(out);
            gout.write(bytes);
            gout.close();

            byte[] byteArray = out.toByteArray();
            if (byteArray.length < bytes.length) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(byteArray.length);
                buffer.put(byteArray);
                buffer.flip();
                gzipped = new ReadableByteBuffer(buffer);
            }
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        String lastModified = HttpDateFormatterHolder.get().format(new Date(new File(path).lastModified()));
        String md5 = MD5.create().update(bytes).asString().toUpperCase();

        return new FileHolder(new ReadableByteBuffer(buffer), md5, lastModified, gzipped);
    }

    public static class FileHolder implements MemoryLimitedCacheWrapper.SizeProvider {
        public final ReadableByteBuffer buffer;
        public final String md5;
        public final String lastModified;
        public final ReadableByteBuffer gzipped;

        public FileHolder(ReadableByteBuffer buffer, String md5, String lastModified, ReadableByteBuffer gzipped) {
            this.buffer = buffer;
            this.md5 = md5;
            this.lastModified = lastModified;
            this.gzipped = gzipped;
        }

        @Override
        public long size() {
            return buffer.length() + (gzipped != null ? gzipped.length() : 0);
        }

        public long length(Request request) {
            if (gzipped != null && request.header(Header.KEY_ACCEPT_ENCODING, "").contains("gzip"))
                return gzipped.length();
            else
                return buffer.length();
        }

        public ReadableByteBuffer getBuffer(Request request, Response response) {
            if (gzipped != null && request.header(Header.KEY_ACCEPT_ENCODING, "").contains("gzip")) {
                response.appendHeader(Header.KV_CONTENT_ENCODING_GZIP);
                return gzipped;
            }

            return buffer;
        }
    }

    public FileHolder getFileHolder(File file) {
        return file.length() <= maxCachedFileSize ? filesCache.get(file.getAbsolutePath()) : null;
    }

    public Response makeRangeResponse(Request request, Response response, File file) {
        return makeRangeResponse(request, response, file, (res, f) -> request.connection().getServer().getMimeProvider().provideContentType(res, f));
    }

    public Response makeRangeResponse(Request request, Response response, File file, BiConsumer<Response, File> contentTypeProvider) {
        response.appendHeader(Header.KEY_ACCEPT_RANGES, Header.VALUE_BYTES);

        Range range;
        String rangeHeader = request.header(Header.KEY_RANGE);
        FileHolder fileHolder = getFileHolder(file);
        ReadableByteBuffer buffer = null;

        if (rangeHeader != null) {
            long length = fileHolder == null ? file.length() : fileHolder.length(request);
            range = new Range(rangeHeader, length);
            if (!range.isValid()) {
                response.setStatus(Status._416);
                return response;
            }

            response.setStatus(Status._206);
            response.appendHeader(Header.KEY_CONTENT_RANGE, range.toString());
        } else {
            Date modifiedSince = request.headerDate(Header.KEY_IF_MODIFIED_SINCE);
            if (modifiedSince != null && modifiedSince.getTime() >= file.lastModified())
                return response.status(Status._304);

            range = new Range(0, file.length() - 1, file.length());
        }

        if (fileHolder != null) {
            if (fileHolder.md5.equals(request.header(Header.KEY_IF_NONE_MATCH)))
                return response.status(Status._304);

            buffer = fileHolder.getBuffer(request, response);

            response.appendHeader(Header.KEY_ETAG, fileHolder.md5);
            response.appendHeader(Header.KEY_LAST_MODIFIED, fileHolder.lastModified);
            response.appendHeader(Header.KEY_CACHE_CONTROL, MAX_AGE_1_YEAR);
            if (range.to >= buffer.length())
                range = new Range(0, buffer.length() - 1, buffer.length());
        } else {
            response.appendHeader(Header.KEY_LAST_MODIFIED, HttpDateFormatterHolder.get().format(new Date(file.lastModified())));
        }

        response.appendHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(range.length()));
        response.appendHeader(Header.KEY_CONNECTION, Header.VALUE_KEEP_ALIVE);

        contentTypeProvider.accept(response, file);

        try {
            if (buffer != null)
                response.setBody(buffer.subBuffer((int) range.from, (int) range.length()));
            else
                response.setBody(new ReadableFile(file, range.from, range.length()));
        } catch (IOException e) {
            throw Unchecked.rethrow(e);
        }
        return response;
    }

    public static class Range {
        final long from;
        final long to;
        final long total;

        private Range(long from, long to, long total) {
            this.from = from;
            this.to = to;
            this.total = total;
        }

        public Range(String range, long length) {
            long to;
            long from;
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

            this.to = to;
            this.from = from;
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
