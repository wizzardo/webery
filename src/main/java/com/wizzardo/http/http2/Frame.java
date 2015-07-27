package com.wizzardo.http.http2;

/**
 * Created by wizzardo on 27.07.15.
 */
public class Frame {

    public enum Type {
        DATA(0),
        HEADERS(1),
        PRIORITY(2),
        RST_STREAM(3),
        SETTINGS(4),
        PUSH_PROMISE(5),
        PING(6),
        GOAWAY(7),
        WINDOW_UPDATE(8),
        CONTINUATION(9);

        public final int value;

        Type(int value) {
            this.value = value;
        }
    }

    private static final int HEADER_LENGTH = 9;
    private static final int MAX_LENGTH = 1 << 14;
    private final int offset;
    private final int length;
    protected byte[] data;

    public Frame(byte[] data) {
        this(data, 0, data.length);
    }

    public Frame(byte[] data, int offset, int length) {
        if (length >= MAX_LENGTH)
            throw new IllegalArgumentException("length >= MAX_LENGTH");

        byte[] bytes = new byte[HEADER_LENGTH + length];
        System.arraycopy(data, offset, bytes, HEADER_LENGTH, length);
        this.data = bytes;
        this.offset = HEADER_LENGTH;
        this.length = length;

        data[2] = (byte) length;
        data[1] = (byte) ((length >> 8) & 0x3f); //14 bit length - max
    }

    public Frame type(int type) {
        data[3] = (byte) type;
        return this;
    }

    public Frame type(Type type) {
        return type(type.value);
    }

    public Frame flag(int flag) {
        data[4] = (byte) flag;
        return this;
    }

    public Frame streamId(int id) {
        data[5] = (byte) ((id >> 24) & 0x7f);
        data[6] = (byte) (id >> 16);
        data[7] = (byte) (id >> 8);
        data[8] = (byte) id;
        return this;
    }


}
