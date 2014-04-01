package com.wizzardo.httpserver.response;

import com.wizzardo.epoll.readable.ReadableBytes;
import com.wizzardo.httpserver.ReadableBuilder;
import com.wizzardo.httpserver.request.Header;
import simplehttpserver.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author: wizzardo
 * Date: 3/31/14
 */
public class Response {
    private static final byte[] LINE_SEPARATOR = "\r\n".getBytes();
    private static final byte[] HEADER_SEPARATOR = ": ".getBytes();
    private Map<byte[], byte[]> headers = new LinkedHashMap<byte[], byte[]>();

    private HttpResponse.Status status = HttpResponse.Status._200;
    private byte[] body;

    public Response setBody(byte[] body) {
        this.body = body;
        return this;
    }

    public Response appendHeader(String key, String value) {
        headers.put(key.getBytes(), value.getBytes());
        return this;
    }

    public Response appendHeader(Header key, String value) {
        headers.put(key.bytes, value.getBytes());
        return this;
    }

    public Response appendHeader(Header key, Header value) {
        headers.put(key.bytes, value.bytes);
        return this;
    }

    public ReadableBytes toReadableByteArray() {
        ReadableBuilder builder = new ReadableBuilder(status.header);
        for (Map.Entry<byte[], byte[]> header : headers.entrySet()) {
            builder.append(header.getKey())
                    .append(HEADER_SEPARATOR)
                    .append(header.getValue())
                    .append(LINE_SEPARATOR);
        }

        builder.append(Header.KEY_CONTENT_LENGTH.bytes)
                .append(HEADER_SEPARATOR)
                .append(String.valueOf(body.length).getBytes())
                .append(LINE_SEPARATOR);

        builder.append(LINE_SEPARATOR);
        builder.append(body);
        return builder;
    }
}
