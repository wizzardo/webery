package com.wizzardo.http.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * @author: wizzardo
 * Date: 06.10.14
 */
class Frame {
    static final int MAX_HEADER_LENGTH = 14;

    static final int FINAL_FRAME = 1 << 7;
    static final int MASKED = 1 << 7;
    static final int RSV1 = 1 << 6;
    static final int RSV2 = 1 << 5;
    static final int RSV3 = 1 << 4;
    static final int OPCODE = 0x0f;
    static final byte OPCODE_CONTINUATION_FRAME = 0;
    static final byte OPCODE_TEXT_FRAME = 1;
    static final byte OPCODE_BINARY_FRAME = 2;
    static final byte OPCODE_CONNECTION_CLOSE = 8;
    static final byte OPCODE_PING = 9;
    static final byte OPCODE_PONG = 10;
    static final int LENGTH_FIRST_BYTE = 0x7f;

    private static final Random RANDOM = new Random();

    private boolean finalFrame = true;
    private byte rsv1, rsv2, rsv3;
    private byte opcode = OPCODE_TEXT_FRAME;
    private boolean masked;
    private int length;
    private byte[] maskingKey;
    private boolean complete;
    private byte[] data;
    private int offset;
    private int read;
    private boolean readHeaders = false;

    public Frame(byte[] data, int offset, int length) {
        byte[] bytes = new byte[MAX_HEADER_LENGTH + length];
        System.arraycopy(data, offset, bytes, MAX_HEADER_LENGTH, length);
        this.data = bytes;
        this.offset = MAX_HEADER_LENGTH;
        this.length = length;
    }

    public Frame(byte opCode) {
        this.opcode = opCode;
    }

    public Frame() {
    }

    @Override
    public String toString() {
        return new String(data, offset, length);
    }

    public byte[] getData() {
        return data;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public void write(OutputStream out) throws IOException {
        if (data == null)
            data = new byte[MAX_HEADER_LENGTH];

        int headerOffset = getHeader(data);
        out.write(data, headerOffset, length + MAX_HEADER_LENGTH - headerOffset);
    }

    private void mask(byte[] data, byte[] mask, int offset, int length) {
        for (int i = offset; i < length + offset; i++) {
            data[i] = (byte) (data[i] ^ mask[(i - offset) % 4]);
        }
    }

    public Frame unmask() {
        if (!masked)
            return this;

        masked = false;
        mask(data, maskingKey, offset, length);
        return this;
    }

    public Frame mask() {
        if (masked)
            return this;

        masked = true;
        if (maskingKey == null)
            maskingKey = intToBytes(RANDOM.nextInt());
        mask(data, maskingKey, offset, length);
        return this;
    }

    private byte[] intToBytes(int i) {
        return intToBytes(i, new byte[4], 0);
    }

    private byte[] intToBytes(int i, byte[] bytes, int offset) {
        bytes[offset] = (byte) ((i >> 24) & 0xff);
        bytes[offset + 1] = (byte) ((i >> 16) & 0xff);
        bytes[offset + 2] = (byte) ((i >> 8) & 0xff);
        bytes[offset + 3] = (byte) (i & 0xff);
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

        if (readHeaders) {
            int r = Math.min(this.length - read, length);
            System.arraycopy(bytes, offset, data, read + this.offset, r);
            read += r;
            if (this.length == read)
                complete = true;
            return r;
        } else {
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
                maskingKey = new byte[]{bytes[offset + r], bytes[offset + r + 1], bytes[offset + r + 2], bytes[offset + r + 3]};
                r += 4;
            }

            complete = length - r >= this.length;

            data = new byte[MAX_HEADER_LENGTH + this.length];
            read = Math.min(length - r, this.length);
            System.arraycopy(bytes, r, data, MAX_HEADER_LENGTH, read);
            this.offset = MAX_HEADER_LENGTH;
            readHeaders = true;
            return read + r;
        }
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

    private int calculateHeadersSize(int dataLength, boolean masked) {
        if (dataLength <= 125)
            return 2 + (masked ? 4 : 0);
        else if (dataLength < 65536)
            return 4 + (masked ? 4 : 0);
        else
            return 10 + (masked ? 4 : 0);
    }

    public int getHeader(byte[] header) {
        int headerOffset = MAX_HEADER_LENGTH - calculateHeadersSize(length, masked);
        int value = opcode;
        if (finalFrame)
            value |= FINAL_FRAME;
        header[headerOffset] = (byte) value;

        value = length;
        if (value <= 125) {
            if (masked)
                value |= MASKED;
            header[headerOffset + 1] = (byte) value;
        } else if (value < 65536) {
            value = 126;
            if (masked)
                value |= MASKED;
            header[headerOffset + 1] = (byte) value;
            header[headerOffset + 2] = (byte) (length >> 8);
            header[headerOffset + 3] = (byte) length;
        } else {
            value = 127;
            if (masked)
                value |= MASKED;
            header[headerOffset + 1] = (byte) value;
            header[headerOffset + 6] = (byte) (length >> 24);
            header[headerOffset + 7] = (byte) (length >> 16);
            header[headerOffset + 8] = (byte) (length >> 8);
            header[headerOffset + 9] = (byte) length;
        }

        if (masked) {
            header[MAX_HEADER_LENGTH - 4] = maskingKey[0];
            header[MAX_HEADER_LENGTH - 3] = maskingKey[1];
            header[MAX_HEADER_LENGTH - 2] = maskingKey[2];
            header[MAX_HEADER_LENGTH - 1] = maskingKey[3];
        }

        return headerOffset;
    }

    public boolean isPing() {
        return opcode == OPCODE_PING;
    }

    public boolean isPong() {
        return opcode == OPCODE_PONG;
    }

    public void setOpcode(byte opcode) {
        this.opcode = opcode;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setIsFinalFrame(boolean isFinalFrame) {
        this.finalFrame = isFinalFrame;
    }

    public boolean isClose() {
        return opcode == OPCODE_CONNECTION_CLOSE;
    }

    public byte[] getFrameBytes() {
        if (data == null)
            data = new byte[MAX_HEADER_LENGTH];

        getHeader(data);

        return data;
    }

    public int getFrameOffset() {
        return MAX_HEADER_LENGTH - calculateHeadersSize(length, masked);
    }

    public int getFrameLength() {
        return calculateHeadersSize(length, masked) + length;
    }
}
