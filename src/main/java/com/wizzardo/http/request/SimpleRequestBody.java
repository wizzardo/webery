package com.wizzardo.http.request;

/**
 * @author: wizzardo
 * Date: 7/29/14
 */
public class SimpleRequestBody {
    private byte[] bytes;
    private int position = 0;

    public SimpleRequestBody(int size) {
        bytes = new byte[size];
    }

    public int read(byte[] bytes, int offset, int length) {
        int l = Math.min(length, remains());
        System.arraycopy(bytes, offset, this.bytes, position, l);
        position += l;
        return l;
    }

    public int remains() {
        return bytes.length - position;
    }

    public boolean isReady() {
        return remains() == 0;
    }

    public byte[] bytes() {
        return bytes;
    }
}
