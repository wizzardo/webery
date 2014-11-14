package com.wizzardo.http.response;

import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableByteBuffer;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.request.Header;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author: wizzardo
 * Date: 3/31/14
 */
public class Response {
    protected static final byte[] LINE_SEPARATOR = "\r\n".getBytes();
    protected static final byte[] HEADER_SEPARATOR = ": ".getBytes();

    protected static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd-MMM-yyyy kk:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format;
        }
    };

    protected boolean processed = false;
    protected Status status = Status._200;
    protected ReadableData body;
    protected ReadableData staticResponse;

    private byte[][] headers = new byte[20][];
    private int headersCount = 0;

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

    public Response setBody(byte[] body) {
        return setBody(new ReadableByteArray(body));
    }

    public Response setBody(ReadableData body) {
        this.body = body;
        setHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(body.length()));
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
        return appendHeader(key.getBytes(), value.getBytes());
    }

    public Response appendHeader(Header key, String value) {
        return appendHeader(key.bytes, value.getBytes());
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

    public String header(String key) {
        return header(key.getBytes());
    }

    public String header(Header key) {
        return header(key.bytes);
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

    protected ReadableBuilder buildResponse() {
        ReadableBuilder builder = new ReadableBuilder(statusToBytes());
        for (int i = 0; i < headersCount; i += 2) {
            builder.append(headers[i])
                    .append(HEADER_SEPARATOR)
                    .append(headers[i + 1])
                    .append(LINE_SEPARATOR);
        }

        builder.append(LINE_SEPARATOR);
        if (body != null)
            builder.append(body);
        return builder;
    }

    protected byte[] statusToBytes() {
        return status.header;
    }

    public boolean isProcessed() {
        return processed;
    }

    public OutputStream getOutputStream(HttpConnection connection) {
        if (!processed) {
            connection.getOutputStream();
            connection.write(toReadableBytes());
            processed = true;
        }

        return connection.getOutputStream();
    }

    public void setCookie(String key, String value, String path) {
        Date expdate = new Date();
        expdate.setTime(expdate.getTime() + (3600 * 1000));
//Set-Cookie: RMID=732423sdfs73242; expires=Fri, 31 Dec 2010 23:59:59 GMT; path=/; domain=.example.net
        appendHeader(Header.KEY_SET_COOKIE, key + "=" + value + "; expires=" + dateFormatThreadLocal.get().format(expdate) + "; path=" + path);
    }

    public ReadableByteBuffer buildStaticResponse() {
        return new ReadableByteBuffer(new ByteBufferWrapper(toReadableBytes()));
    }

    public Response setStaticResponse(ReadableByteBuffer data) {
        staticResponse = data;
        return this;
    }
}
