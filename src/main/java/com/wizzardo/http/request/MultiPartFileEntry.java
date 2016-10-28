package com.wizzardo.http.request;

import com.wizzardo.tools.io.FileTools;

import java.io.*;

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
    public OutputStream outputStream() throws IOException {
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

    @Override
    public boolean save(File file) {
        return this.file.renameTo(file);
    }

    @Override
    public InputStream inputStream() throws IOException {
        return new FileInputStream(file);
    }

    public File getFile() {
        return file;
    }

    @Override
    public void delete() {
        file.delete();
    }
}
