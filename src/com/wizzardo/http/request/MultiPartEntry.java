package com.wizzardo.http.request;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: wizzardo
 * Date: 15.10.14
 */
public abstract class MultiPartEntry {
    private String name;
    private Map<String, String> headers = new HashMap<>();

    public MultiPartEntry(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public String header(String key) {
        return headers.get(key);
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String contentType() {
        return headers.get("Content-Type");
    }

    public String fileName() {
        return null;
    }

    void header(String key, String value) {
        headers.put(key, value);
    }

    public String asString() {
        return new String(asBytes());
    }

    public abstract int length();

    public abstract byte[] asBytes();

    abstract OutputStream outputStream() throws IOException;
}
