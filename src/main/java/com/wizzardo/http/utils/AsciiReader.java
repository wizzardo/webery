package com.wizzardo.http.utils;

import com.wizzardo.tools.reflection.StringReflection;

/**
 * Created by wizzardo on 29.12.14.
 */
public class AsciiReader {

    public static String read(byte[] bytes) {
        return read(bytes, 0, bytes.length);
    }

    public static String read(byte[] bytes, int offset, int length) {
        if (length <= 0)
            return new String();

        int h = 0;
        int k;
        char[] data = new char[length];
        for (int i = 0; i < length; i++) {
            data[i] = (char) (k = (bytes[offset + i] & 0xff));
            h = 31 * h + k;
        }

        return StringReflection.createString(data, h);
    }

    public static String read(byte[] bytes, int offset, int length, int hash) {
//            return read(bytes, offset, length);
        if (length == 0)
            return new String();

        char[] data = new char[length];
        for (int i = 0; i < length; i++) {
            data[i] = (char) (bytes[offset + i] & 0xff);
        }

        return StringReflection.createString(data, hash);
    }

    public static String read(byte[] buffer, int bufferLength, byte[] bytes, int offset, int length) {
        char[] data = new char[bufferLength + length];

        int h = 0;
        int k;
        for (int i = 0; i < bufferLength; i++) {
            data[i] = (char) (k = (buffer[i] & 0xff));
            h = 31 * h + k;
        }
        for (int i = 0; i < length; i++) {
            data[i + bufferLength] = (char) (k = (bytes[offset + i] & 0xff));
            h = 31 * h + k;
        }
        return StringReflection.createString(data, h);
    }

    public static byte[] write(String s) {
        byte[] bytes = new byte[s.length()];
        write(s, bytes, 0);
        return bytes;
    }

    public static int write(String s, byte[] bytes, int offset) {
        char[] chars = StringReflection.chars(s);
        int l = chars.length + offset;
        for (int i = offset; i < l; i++) {
            bytes[i] = (byte) chars[i - offset];
        }
        return l;
    }
}
