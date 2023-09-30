package com.wizzardo.http.request;

/**
 * @author: wizzardo
 * Date: 7/29/14
 */
public class SimpleRequestBody implements RequestBody {
    protected byte[] bytes;
    protected int position = 0;

    public SimpleRequestBody(int size) {
        bytes = new byte[size];
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
        int l = Math.min(length, remains());
        System.arraycopy(bytes, offset, this.bytes, position, l);
        position += l;
        return l;
    }

    @Override
    public int remains() {
        return bytes.length - position;
    }

    @Override
    public boolean isReady() {
        return remains() == 0;
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public int offset() {
        return 0;
    }

    @Override
    public int length() {
        return bytes.length;
    }

    @Override
    public String toString() {
        return "SimpleRequestBody{" +
                "length=" + length() +
                ", isReady=" + isReady() +
                ", position=" + position +
                '}';
    }
}
