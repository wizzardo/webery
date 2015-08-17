package com.wizzardo.http.http2.hpack;

import com.wizzardo.http.utils.AsciiReader;
import com.wizzardo.tools.reflection.StringReflection;

/**
 * Created by wizzardo on 08.08.15.
 */
public class HpackReader {

    public static int encode(String s, boolean compress, byte[] bytes, int offsetBits) {
        if (compress)
            return encodeWithCompression(s, bytes, offsetBits);
        else
            return encodeWithoutCompression(s, bytes, offsetBits);
    }

    public static int encodeWithoutCompression(String s, byte[] bytes, int offsetBits) {
        if (offsetBits % 8 != 0)
            return -1;

        int index = offsetBits >> 3;
        bytes[index] = 0;
        offsetBits = encode(s.length(), bytes, offsetBits + 1);
        offsetBits = AsciiReader.write(s, bytes, offsetBits >> 3) << 3;
        return offsetBits;
    }

    public static int encodeWithCompression(String s, byte[] bytes, int offsetBits) {
        if (offsetBits % 8 != 0)
            return -1;

        int index = offsetBits >> 3;
        int l = s.length();
        if (l <= 160) { //average compressed length for one byte
            int offset = StaticHttp2HuffmanTable.huffman.encode(bytes, (offsetBits + 8) >> 3, StringReflection.chars(s));
            int k = 8 - offset + ((offset >> 3) << 3);
            if (k != 8) {
                bytes[offset >> 3] = (byte) (bytes[offset >> 3] | (1 << k) - 1);
                offset += k;
            }
            if ((offset >> 3) > 127) {
                //todo: shift data
            }

            bytes[index] = (byte) 128;
            encode((offset - offsetBits - 8) >> 3, bytes, offsetBits + 1);
            return offset;
        } else {
            //todo: calculate optimal size and encode
        }
        return offsetBits;
    }

    public static int encode(int i, byte[] bytes, int offsetBits) {
        int prefix = 8 - (offsetBits % 8);
        int maxPrefix = (1 << prefix) - 1;
        int index = offsetBits >> 3;

        if (i < maxPrefix) {
            int a = bytes[index];
            a = a & (((1 << (8 - prefix)) - 1) << prefix);
            bytes[index] = (byte) (a | i);
            return offsetBits + prefix;
        }

        int a = bytes[index];
        bytes[index++] = (byte) (a | maxPrefix);

        i -= maxPrefix;
        while (i >= 128) {
            a = 128 + (i % 128);
            bytes[index++] = (byte) a;
            i = i >> 7;
        }

        bytes[index++] = (byte) i;

        return index << 3;
    }

    public static IntDecodingResult decode(byte[] bytes, int offsetBits) {
        int prefix = 8 - (offsetBits % 8);
        int maxPrefix = (1 << prefix) - 1;
        int index = offsetBits >> 3;

        int i = bytes[index] & maxPrefix;
        if (i != maxPrefix)
            return new IntDecodingResult(i, (index + 1) << 3);

        long l = maxPrefix;
        int a;
        index++;

        int count = 0;
        do {
            a = bytes[index++] & 0xff;
            l += (a & 127) << count;
            count += 7;
        } while ((a & 128) == 128);

        if (count > 64)
            throw new IllegalStateException("can not decode numbers with length > 64 bits");

        return new IntDecodingResult(l, index << 3);
    }

    public static class IntDecodingResult {
        public final long value;
        public final int offset;

        public IntDecodingResult(long value, int offset) {
            this.value = value;
            this.offset = offset;
        }
    }
}
