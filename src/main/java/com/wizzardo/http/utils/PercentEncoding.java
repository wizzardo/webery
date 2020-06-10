package com.wizzardo.http.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by Mikhail Bobrutskov on 20.08.17.
 */
public class PercentEncoding {

    static byte[] mapping;

    static {
        mapping = new byte[128];
        Arrays.fill(mapping, (byte) 128);
        mapping['0'] = 0;
        mapping['1'] = 1;
        mapping['2'] = 2;
        mapping['3'] = 3;
        mapping['4'] = 4;
        mapping['5'] = 5;
        mapping['6'] = 6;
        mapping['7'] = 7;
        mapping['8'] = 8;
        mapping['9'] = 9;

        mapping['a'] = 10;
        mapping['b'] = 11;
        mapping['c'] = 12;
        mapping['d'] = 13;
        mapping['e'] = 14;
        mapping['f'] = 15;

        mapping['A'] = 10;
        mapping['B'] = 11;
        mapping['C'] = 12;
        mapping['D'] = 13;
        mapping['E'] = 14;
        mapping['F'] = 15;
    }

    public static int decode(byte[] bytes, int from, int to) {
        int position = from;
        int i = from;
        try {
            while (i < to) {
                byte b = bytes[i];
                if (b == '%') {
                    if (i + 2 >= to)
                        throw new IllegalStateException("Unexpected end of the string");

                    byte value = (byte) ((getHexValue(bytes[++i]) << 4) + getHexValue(bytes[++i]));
                    bytes[position++] = value;
                } else if (b == '+') {
                    bytes[position++] = ' ';
                } else if (position == i) {
                    position++;
                } else {
                    bytes[position++] = b;
                }

                i++;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot decode string '" + new String(bytes, from, to - from, StandardCharsets.UTF_8) + "'", e);
        }
        return position - from;
    }

    public static int getHexValue(int c) {
        if (c >= 128 || c < 0)
            throw new IllegalStateException("unexpected char for hex value: " + (char) c);

        c = mapping[c];
        if (c == 128)
            throw new IllegalStateException("unexpected char for hex value");

        return c;
    }
}
