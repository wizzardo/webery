package com.wizzardo.http.template;

import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public class BytesHolder implements Renderable {
    private byte[] bytes;

    public BytesHolder(byte[] bytes) {
        this.bytes = bytes;
    }

    public BytesHolder(String s) {
        this.bytes = s.getBytes(StandardCharsets.UTF_8);
    }

    public RenderResult get(Map<String, Object> model) {
        return new RenderResult(bytes);
    }

    @Override
    public String toString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
