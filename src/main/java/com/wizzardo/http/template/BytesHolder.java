package com.wizzardo.http.template;

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

    public void append(byte[] bytes) {
        byte[] b = new byte[bytes.length + this.bytes.length];
        System.arraycopy(this.bytes, 0, b, 0, this.bytes.length);
        System.arraycopy(bytes, 0, b, this.bytes.length, bytes.length);
        this.bytes = b;
    }

    public void append(String s) {
        append(s.getBytes(StandardCharsets.UTF_8));
    }
}
