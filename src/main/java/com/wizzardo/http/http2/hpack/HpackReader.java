package com.wizzardo.http.http2.hpack;

/**
 * Created by wizzardo on 08.08.15.
 */
public class HpackReader {

    public static int encode(int i, byte[] bytes, int offsetBits) {
        int prefix = 8 - (offsetBits % 8);
        int maxPrefix = (1 << prefix) - 1;
        int index = offsetBits / 8;

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
            i /= 128;
        }

        bytes[index++] = (byte) i;

        return index * 8;
    }
}
