package com.wizzardo.http.response;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.epoll.readable.*;
import com.wizzardo.http.*;
import com.wizzardo.http.html.Renderer;
import com.wizzardo.http.html.Tag;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.utils.AsciiReader;
import com.wizzardo.http.utils.StringBuilderThreadLocalHolder;
import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
import com.wizzardo.tools.misc.Unchecked;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author: wizzardo
 * Date: 3/31/14
 */
public class Response {
    protected static final byte[] LINE_SEPARATOR = "\r\n".getBytes();
    protected static final byte[] HEADER_SEPARATOR = ": ".getBytes();
    protected static final byte[] EMPTY = new byte[0];

    protected boolean committed = false;
    protected Status status = Status._200;
    protected ReadableData body;
    protected ReadableData staticResponse;

    private byte[][] headers = new byte[8][];
    private int headersCount = 0;
    private ReadableDirectByteBuffer[] headersStatic = new ReadableDirectByteBuffer[8];
    private int headersStaticCount = 1;
    private boolean hasBody = true;

    protected static final StringBuilderThreadLocalHolder stringBuilder = new StringBuilderThreadLocalHolder();
    protected boolean async;

    static ReadableDirectByteBuffer[] lengths = new ReadableDirectByteBuffer[1024];

    static {
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = new ReadableDirectByteBuffer(new ByteBufferWrapper(("Content-Length: " + i + "\r\n").getBytes()));
        }
    }

    public void reset() {
        hasBody = true;
        async = false;
        headersCount = 0;
        headersStaticCount = 1;
        body = null;
        staticResponse = null;
        committed = false;
        status = Status._200;
    }

    public Response body(String s) {
        return body(s.getBytes());
    }

    public Response body(byte[] body) {
        return body(new ReadableByteArray(body));
    }

    public Response body(ReadableData body) {
        return setBody(body);
    }

    public Response setBody(String s) {
        return setBody(s.getBytes());
    }

    public Response setBody(Tag tag) {
        ExceptionDrivenStringBuilder sb = stringBuilder.get();
        tag.render(Renderer.create(sb));
        return setBody(sb.toString());
    }

    public Response setBody(File file) throws IOException {
        return setBody(file, (String) null);
    }

    public Response setBody(File file, MimeProvider mimeProvider) throws IOException {
        return setBody(file, mimeProvider.getMimeType(file.getName()));
    }

    public Response setBody(File file, String contentType) throws IOException {
        if (file.length() < 1024)
            appendHeader(lengths[(int) file.length()]);
        else
            appendHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(file.length()));
        appendHeader(Header.KEY_LAST_MODIFIED, HttpDateFormatterHolder.get().format(new Date(file.lastModified())));
        if (contentType != null)
            appendHeader(Header.KEY_CONTENT_TYPE, contentType);
        return setBody(new ReadableFile(file, 0, file.length()));
    }

    public Response setBody(byte[] body) {
        return setBody(new ReadableByteArray(body));
    }

    public Response setBody(ReadableData body) {
        this.body = body;
        if (body.length() < 1024)
            appendHeader(lengths[(int) body.length()]);
        else
            setHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(body.length()));
        return this;
    }

    public Response setHasBody(boolean hasBody) {
        this.hasBody = hasBody;
        return this;
    }

    public byte[] body() {
        return getBody();
    }

    public byte[] getBody() {
        if (body == null)
            return null;

        byte[] bytes = new byte[(int) body.length()];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        body.read(bb);
        body.unread(bytes.length);
        return bytes;
    }

    public Response status(Status status) {
        return setStatus(status);
    }

    public Response setStatus(Status status) {
        this.status = status;
        return this;
    }

    public long contentLength() {
        return body != null ? body.length() : 0;
    }

    public Status status() {
        return status;
    }

    public void setHeader(String key, String value) {
        setHeader(key.getBytes(), value.getBytes());
    }

    public void setHeader(Header key, String value) {
        setHeader(key.bytes, value.getBytes());
    }

    public void setHeader(Header key, long value) {
        setHeader(key, String.valueOf(value));
    }

    public void setHeader(Header key, Header value) {
        setHeader(key.bytes, value.bytes);
    }

    public Response header(String key, String value) {
        return header(key.getBytes(), value.getBytes());
    }

    public Response header(Header key, String value) {
        return header(key.bytes, value.getBytes());
    }

    public Response header(Header key, long value) {
        return header(key, String.valueOf(value));
    }

    public Response header(Header key, Header value) {
        return header(key.bytes, value.bytes);
    }

    public Response header(byte[] key, byte[] value) {
        int i = indexOfHeader(key);
        if (i >= 0)
            headers[i + 1] = value;
        else
            appendHeader(key, value);
        return this;
    }

    public void setHeader(byte[] key, byte[] value) {
        header(key, value);
    }

    public Response appendHeader(String key, String value) {
        return appendHeader(AsciiReader.write(key), AsciiReader.write(value));
    }

    public Response appendHeader(Header key, String value) {
        return appendHeader(key.bytes, AsciiReader.write(value));
    }

    public Response appendHeader(Header key, Header value) {
        return appendHeader(key.bytes, value.bytes);
    }

    public Response appendHeader(byte[] key, byte[] value) {
        if (headersCount + 1 >= headers.length)
            increaseHeadersSize();

        headers[headersCount++] = key;
        headers[headersCount++] = value;

        return this;
    }

    public Response appendHeader(ReadableDirectByteBuffer value) {
        if (headersStaticCount + 1 >= headersStatic.length) {
            ReadableDirectByteBuffer[] temp = new ReadableDirectByteBuffer[headersStatic.length * 3 / 2];
            System.arraycopy(headersStatic, 0, temp, 0, headersStatic.length);
            headersStatic = temp;
        }

        headersStatic[headersStaticCount++] = value;

        return this;
    }

    /**
     * @param header must be a header string 'key: value\r\n'
     */
    public Response appendHeader(byte[] header) {
        appendHeader(header, EMPTY);
        return this;
    }

    /**
     * @param header must be one of KV_ values of {@link Header}
     */
    public Response appendHeader(Header header) {
        if (!header.complete)
            throw new IllegalStateException("header must be one of KV_ values of Header");

        appendHeader(header.buffer);
        return this;
    }

    public String header(String key) {
        return header(key.getBytes());
    }

    public String header(Header key) {
        String value = header(key.bytes);
        if (value != null)
            return value;

        for (int i = 1; i < headersStaticCount; i++)
            if (key.buffer.bufferEquals(headersStatic[i]))
                return key.name();

        return null;
    }

    public String header(byte[] key) {
        int i = indexOfHeader(key, 0);
        if (i != -1)
            return new String(headers[i + 1]);
        return null;
    }

    public List<String> headers(String key) {
        return headers(key.getBytes());
    }

    public List<String> headers(Header key) {
        return headers(key.bytes);
    }

    public List<String> headers(byte[] key) {
        List<String> l = new ArrayList<>();
        int i = -2;
        while ((i = indexOfHeader(key, i + 2)) != -1)
            l.add(new String(headers[i + 1]));
        return l;
    }

    public Set<String> headerNames() {
        Set<String> l = new LinkedHashSet<>();
        for (int i = 0; i < headersCount; i += 2) {
            l.add(new String(headers[i]));
        }
        return l;
    }

    public void headersReset() {
        headersCount = 0;
        headersStaticCount = 1;
    }

    public boolean containsHeader(String key) {
        return containsHeader(key.getBytes());
    }

    public boolean containsHeader(Header key) {
        return containsHeader(key.bytes);
    }

    public boolean containsHeader(byte[] key) {
        return indexOfHeader(key) != -1;
    }

    private int indexOfHeader(byte[] key) {
        return indexOfHeader(key, 0);
    }

    private int indexOfHeader(byte[] key, int offset) {
        for (int i = offset; i < headersCount; i += 2)
            if (Arrays.equals(key, headers[i]))
                return i;
        return -1;
    }

    private void increaseHeadersSize() {
        byte[][] temp = new byte[headers.length * 3 / 2][];
        System.arraycopy(headers, 0, temp, 0, headers.length);
        headers = temp;
    }

    public ReadableData toReadableBytes() {
        if (staticResponse != null)
            return staticResponse;
        return buildResponse();
    }

    static class ReadableBuilderProxy extends ReadableBuilder {

        final static ByteBufferWrapper EMPTY = new ByteBufferWrapper(0);
        ByteBufferWrapper bb = EMPTY;

        ReadableBuilderProxy(ByteBufferWrapper bb) {
            super(0);
            this.bb = bb;
        }

        ReadableBuilderProxy() {
        }

        @Override
        public int read(ByteBuffer byteBuffer) {
            if (position >= partsCount)
                return 0;

            ReadableData[] parts = this.parts;
            ReadableData data = parts[position];
            int r = data.read(byteBuffer);
            while (position < partsCount - 1 && data.isComplete()) {
                data = parts[++position];
                if (!byteBuffer.hasRemaining())
                    break;
                r += data.read(byteBuffer);
            }

            return r;
        }

        @Override
        public ReadableBuilder append(byte[] bytes, int offset, int length) {
            if (bb.buffer().remaining() > length) {
                bb.put(bytes, offset, length);
                return this;
            } else
                return super.append(bytes, offset, length);
        }

        @Override
        public ReadableBuilder append(ReadableData readableData) {
            if (bb.buffer().remaining() > readableData.length()) {
                readableData.read(bb);

                try {
                    readableData.close();
                } catch (IOException e) {
                    throw Unchecked.rethrow(e);
                }
                return this;
            } else
                return super.append(readableData);
        }

        public ReadableBuilder append(ReadableDirectByteBuffer readableData) {
            if (bb.buffer().remaining() > readableData.length()) {
                ReadableDirectByteBuffer.copy(bb, readableData);
                return this;
            } else
                return super.append(readableData.copy());
        }

        public ReadableBuilderProxy append(ReadableDirectByteBuffer s1, ReadableDirectByteBuffer s2, ReadableDirectByteBuffer s3, ReadableDirectByteBuffer s4, ReadableDirectByteBuffer s5) {
            if (bb.buffer().remaining() >= s1.length() + s2.length() + s3.length() + s4.length() + s5.length()) {
                ReadableDirectByteBuffer.read(bb, s1, s2, s3, s4, s5);
                return this;
            } else {
                super.append(s1.copy());
                super.append(s2.copy());
                super.append(s3.copy());
                super.append(s4.copy());
                super.append(s5.copy());
            }
            return this;
        }
    }

    protected ReadableBuilder buildResponse() {
//        ReadableBuilder builder = new ReadableBuilder();
        return buildResponse(new ReadableBuilderProxy());
    }

    protected ReadableBuilder buildResponse(ReadableBuilderProxy builder) {
//        builder.append(status.buffer());
        {
            ReadableDirectByteBuffer[] headersStatic = this.headersStatic;
            headersStatic[0] = status.buffer;
            int headersStaticCount = this.headersStaticCount;
            int i = 0;
            for (; i <= headersStaticCount - 5; i += 5) {
                builder.append(headersStatic[i], headersStatic[i + 1], headersStatic[i + 2], headersStatic[i + 3], headersStatic[i + 4]);
            }
            for (; i < headersStaticCount; i++) {
                builder.append(headersStatic[i]);
            }
        }

        int headersCount = this.headersCount;
        if (headersCount > 0) {
            byte[][] headers = this.headers;
            for (int i = 0; i < headersCount; i += 2) {
                if (headers[i + 1] == EMPTY)
                    builder.append(headers[i]);
                else
                    builder.append(headers[i])
                            .append(HEADER_SEPARATOR)
                            .append(headers[i + 1])
                            .append(LINE_SEPARATOR);
            }
        }

        builder.append(LINE_SEPARATOR);
        if (body != null) {
            if (hasBody)
                builder.append(body);
            else
                try {
                    body.close();
                } catch (IOException e) {
                    throw Unchecked.rethrow(e);
                }
        }
        return builder;
    }

    protected byte[] statusToBytes() {
        return status.bytes;
    }

    public boolean isCommitted() {
        return committed;
    }

    public <H extends AbstractHttpServer, Q extends Request, S extends Response> OutputStream getOutputStream(HttpConnection<H, Q, S> connection) {
//        connection.getOutputStream();
        if (!committed) {
            commit(connection);
            connection.flush();
        }

        return connection.getOutputStream();
    }

    public void commit(HttpConnection connection) {
        commit(connection, (ByteBufferProvider) Thread.currentThread());
    }

//    static int commits = 0;

    public void commit(HttpConnection connection, ByteBufferProvider byteBufferProvider) {
        if (!committed) {
//            connection.write(toReadableBytes(), byteBufferProvider);
//            byteBufferProvider.getBuffer().clear();
            ByteBufferWrapper buffer = byteBufferProvider.getBuffer();
            ReadableBuilderProxy builderProxy = new ReadableBuilderProxy(buffer);
            buildResponse(builderProxy);
//            System.out.println("commits: " + (++commits) + "\tdata:" + buffer.position());
            if (builderProxy.length() > 0) {
                connection.flush();
                if (!connection.hasDataToWrite() && builderProxy.length() <= buffer.capacity()) {
                    do {
                        builderProxy.read(buffer);
                    } while (builderProxy.remains() > 0);
                } else
                    connection.send(builderProxy);
            }
            committed = true;
        }
    }

    public void setCookie(String cookie) {
        appendHeader(Header.KEY_SET_COOKIE, cookie);
    }

    public void setCookie(CookieBuilder cookieBuilder) {
        setCookie(cookieBuilder.build());
    }

    public ReadableByteBuffer buildStaticResponse() {
        return new ReadableByteBuffer(new ByteBufferWrapper(toReadableBytes()));
    }

    public Response setStaticResponse(ReadableByteBuffer data) {
        staticResponse = data;
        return this;
    }

    public void setRedirectTemporarily(String location) {
        status(Status._302);
        header(Header.KEY_LOCATION, location);
    }

    public void setRedirectPermanently(String location) {
        status(Status._301);
        header(Header.KEY_LOCATION, location);
    }

    public void async() {
        async = true;
    }

    public boolean isAsync() {
        return async;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("status: ").append(status.code).append("\n");
        for (int i = 1; i < headersStaticCount; i++) {
            sb.append(this.headersStatic[i].toString());
        }
        byte[][] headers = this.headers;
        for (int i = 0; i < headersCount; i += 2) {
            if (headers[i + 1] == EMPTY)
                sb.append(new String(headers[i], StandardCharsets.UTF_8));
            else
                sb.append(new String(headers[i], StandardCharsets.UTF_8))
                        .append(": ")
                        .append(new String(headers[i + 1], StandardCharsets.UTF_8))
                        .append("\r\n");
        }

        if (body != null) {
            sb.append(new String(body()));
        }

        return sb.toString();
    }
}
