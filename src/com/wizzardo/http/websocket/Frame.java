package com.wizzardo.http.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * @author: wizzardo
 * Date: 06.10.14
 */
public class Frame {
    static final int FINAL_FRAME = 1 << 7;
    static final int MASKED = 1 << 7;
    static final int RSV1 = 1 << 6;
    static final int RSV2 = 1 << 5;
    static final int RSV3 = 1 << 4;
    static final int OPCODE = 0x0f;
    static final int OPCODE_CONTINUATION_FRAME = 0;
    static final int OPCODE_TEXT_FRAME = 1;
    static final int OPCODE_CONNECTION_CLOSE = 8;
    static final int OPCODE_PING = 9;
    static final int OPCODE_PONG = 10;
    static final int LENGTH_FIRST_BYTE = 0x7f;

    private static final Random RANDOM = new Random();

    private boolean finalFrame;
    private byte rsv1, rsv2, rsv3;
    private byte opcode;
    private boolean masked;
    private int length;
    private int maskingKey;
    private boolean complete;
    private byte[] data;
    private int offset;
    private int read;
    private int state = 0;

    public Frame() {
    }

    public Frame(byte[] bytes, int offset, int length) {
        byte b = bytes[offset];
        finalFrame = (b & FINAL_FRAME) != 0;
        rsv1 = (byte) (b & RSV1);
        rsv2 = (byte) (b & RSV2);
        rsv3 = (byte) (b & RSV3);

        opcode = (byte) (b & OPCODE);

        b = bytes[offset + 1];
        masked = (b & MASKED) != 0;
        this.length = b & LENGTH_FIRST_BYTE;
        int r = 2;
        if (this.length == 126) {
            this.length = ((bytes[offset + 2] & 0xff) << 8) + (bytes[offset + 3] & 0xff);
            r += 2;
        } else if (this.length == 127) {
            this.length =
//                        ((long) (bytes[offset + 2] & 0xff) << 56)
//                        + ((long) (bytes[offset + 3] & 0xff) << 48)
//                        + ((long) (bytes[offset + 4] & 0xff) << 40)
//                        + ((long) (bytes[offset + 5] & 0xff) << 32)  // not support long frames
                    +((bytes[offset + 6] & 0xff) << 24)
                            + ((bytes[offset + 7] & 0xff) << 16)
                            + ((bytes[offset + 8] & 0xff) << 8)
                            + (bytes[offset + 9] & 0xff);
            r += 8;
        }
        if (masked) {
            maskingKey = ((bytes[offset + r] & 0xff) << 24)
                    + ((bytes[offset + r + 1] & 0xff) << 16)
                    + ((bytes[offset + r + 2] & 0xff) << 8)
                    + (bytes[offset + r + 3] & 0xff);
            r += 4;
        }

        complete = length - r >= this.length;

        data = new byte[this.length];
        System.arraycopy(bytes, r, data, 0, length - r);
        this.offset = 0;
        read = length - r;
    }

    @Override
    public String toString() {
        return new String(data, offset, length);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public void write(OutputStream out) throws IOException {
        int value = 0;
        value |= FINAL_FRAME;
        value |= OPCODE_TEXT_FRAME;
        out.write(value);

        value = length;
        if (value <= 125) {
            value |= MASKED;
            out.write(value);
        } else if (value < 65536) {
            value |= MASKED;
            out.write(value);
            out.write(length >> 8);
            out.write(length);
        } else {
            value |= MASKED;
            out.write(value);
//                out.write((int) (length >> 56));
//                out.write((int) (length >> 48));
//                out.write((int) (length >> 40));
//                out.write((int) (length >> 32));
            out.write(length >> 24);
            out.write(length >> 16);
            out.write(length);
        }

        byte[] mask = intToBytes(RANDOM.nextInt());
        out.write(mask);
        mask(data, mask, offset, length);
        out.write(data, offset, length);
    }

    private void mask(byte[] data, byte[] mask, int offset, int length) {
        for (int i = offset; i < length + offset; i++) {
            data[i] = (byte) (data[i] ^ mask[(i - offset) % 4]);
        }
    }

    private byte[] intToBytes(int i) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((i >> 24) & 0xff);
        bytes[1] = (byte) ((i >> 16) & 0xff);
        bytes[2] = (byte) ((i >> 8) & 0xff);
        bytes[3] = (byte) (i & 0xff);
        return bytes;
    }

    public void read(InputStream in) throws IOException {
        while (read != length) {
            read += in.read(data, offset + read, (length - read));
        }
        complete = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isFinalFrame() {
        return finalFrame;
    }

    public int read(byte[] bytes, int offset, int length) {
        if (complete)
            return 0;

        int r = Math.min(this.length - read, length);
        System.arraycopy(bytes, offset, data, read, r);
        read += r;
        if (this.length == read)
            complete = true;
        return r;
    }

    public static boolean hasHeaders(byte[] bytes, int offset, int length) {
        if (length >= 2) {
            int b = bytes[offset + 1] & 0xff;
            if ((b & Frame.MASKED) != 0) {
                length -= 4;
                b -= 128;
            }

            if (b <= 125)
                return true;
            else if (b == 126 && length >= 4)
                return true;
            else if (b == 127 && length >= 10)
                return true;

        }
        return false;
    }
}
