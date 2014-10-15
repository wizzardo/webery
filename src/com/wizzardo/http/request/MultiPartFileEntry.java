package com.wizzardo.http.request;

import com.wizzardo.tools.io.FileTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author: wizzardo
 * Date: 15.10.14
 */
public class MultiPartFileEntry extends MultiPartEntry {

    private String filename;
    private File file;

    public MultiPartFileEntry(String name, String filename) throws IOException {
        super(name);
        this.filename = filename;
        file = File.createTempFile("--MultiPartEntry", "--");
    }

    @Override
    public int length() {
        return (int) file.length();
    }

    @Override
    OutputStream outputStream() throws IOException {
        return new FileOutputStream(file);
    }

    @Override
    public String fileName() {
        return filename;
    }

    @Override
    public byte[] asBytes() {
        return FileTools.bytes(file);
    }

    File getFile() {
        return file;
    }
}
