package com.wizzardo.http.request;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

    public void delete() {
    }

    public void header(String key, String value) {
        headers.put(key, value);
    }

    public String asString() {
        return new String(asBytes(), StandardCharsets.UTF_8);
    }

    public abstract int length();

    public abstract byte[] asBytes();

    public abstract boolean save(File file);

    public abstract InputStream inputStream() throws IOException;

    public abstract OutputStream outputStream() throws IOException;

    @Override
    public String toString() {
        return "MultiPartEntry{" +
                "name='" + name + '\'' +
                ", headers=" + headers +
                '}';
    }
}
