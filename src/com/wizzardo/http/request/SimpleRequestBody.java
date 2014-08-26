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

    public boolean read(byte[] bytes, int offset, int length) {
        System.arraycopy(bytes, offset, this.bytes, position, length);
        position += length;
        return remains() == 0;
    }

    public int remains() {
        return bytes.length - position;
    }

    public byte[] bytes() {
        return bytes;
    }
}
