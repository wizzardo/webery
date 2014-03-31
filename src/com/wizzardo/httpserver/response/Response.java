package com.wizzardo.httpserver.response;

import com.wizzardo.epoll.readable.ReadableBytes;
import com.wizzardo.httpserver.ReadableBuilder;
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
    private Map<String, String> headers = new LinkedHashMap<String, String>();

    private HttpResponse.Status status = HttpResponse.Status._200;
    private byte[] body;

    public Response setBody(byte[] body) {
        this.body = body;
        return this;
    }

    public Response appendHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public ReadableBytes toReadableByteArray() {
        ReadableBuilder builder = new ReadableBuilder(status.header);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.append(header.getKey().getBytes())
                    .append(HEADER_SEPARATOR)
                    .append(header.getValue().getBytes())
                    .append(LINE_SEPARATOR);
        }

        builder.append("Content-Length".getBytes())
                .append(HEADER_SEPARATOR)
                .append(String.valueOf(body.length).getBytes())
                .append(LINE_SEPARATOR);

        builder.append(LINE_SEPARATOR);
        builder.append(body);
        return builder;
    }
}
