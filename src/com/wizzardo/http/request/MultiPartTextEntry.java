package com.wizzardo.http.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author: wizzardo
 * Date: 15.10.14
 */
public class MultiPartTextEntry extends MultiPartEntry {
    private ByteArrayOutputStream data;

    public MultiPartTextEntry(String name) throws IOException {
        super(name);
    }

    @Override
    public byte[] asBytes() {
        return data.toByteArray();
    }

    @Override
    public int length() {
        return data.size();
    }

    @Override
    OutputStream outputStream() throws IOException {
        return data = new ByteArrayOutputStream();
    }
}
