package com.wizzardo.http.http2.hpack;

/**
 * Created by wizzardo on 08.08.15.
 */
public class HpackReader {

    public static int encode(int i, byte[] bytes, int offsetBits) {
        int prefix = 8 - (offsetBits % 8);
        int maxPrefix = (1 << prefix) - 1;
        if (i < maxPrefix) {
            int a = bytes[offsetBits / 8];
            a = a & (((1 << (8 - prefix)) - 1) << prefix);
            bytes[offsetBits / 8] = (byte) (a | i);
        }
        return -1;
    }
}
