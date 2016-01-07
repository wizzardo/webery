package com.wizzardo.http.request;

import com.wizzardo.tools.io.FileTools;

import java.io.*;

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
    public boolean save(File file) {
        FileTools.bytes(file, data.toByteArray());
        return true;
    }

    @Override
    public InputStream inputStream() {
        return new ByteArrayInputStream(data.toByteArray());
    }

    @Override
    public int length() {
        return data.size();
    }

    @Override
    public OutputStream outputStream() throws IOException {
        return data = new ByteArrayOutputStream();
    }
}
