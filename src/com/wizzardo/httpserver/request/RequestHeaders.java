package com.wizzardo.httpserver.request;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public class RequestHeaders {

    protected static Map<String, byte[]> keyCache = new HashMap<String, byte[]>() {
        @Override
        public byte[] get(Object key) {
            byte[] bytes = super.get(key);
            if (bytes == null)
                return String.valueOf(key).getBytes();
            return bytes;
        }

        void put(String header) {
            put(header, header.getBytes());
        }

        {
            put(Header.KEY_HOST);
            put(Header.KEY_CONNECTION);
            put(Header.KEY_CACHE_CONTROL);
            put(Header.KEY_ACCEPT);
            put(Header.KEY_PRAGMA);
            put(Header.KEY_USER_AGENT);
            put(Header.KEY_ACCEPT_ENCODING);
            put(Header.KEY_ACCEPT_LANGUAGE);
            put(Header.KEY_COOKIE);
        }
    };


    private byte[] keys = new byte[2048];
    private byte[] values = new byte[2048];
    private int[] keyInfo = new int[100];
    private int[] valueInfo = new int[100];
    private int size = 0;
    private HashMap<String, String> cache = new HashMap<String, String>();

    void put(byte[] key, byte[] value, int offset, int length, byte[] buffer, int fromBuffer) {
        int keyOffset = size > 0 ? keyInfo[size * 2 - 2] + keyInfo[size * 2 - 1] : 0;
        if (keyInfo.length <= size * 2) {
            int[] arr = new int[(int) (keyInfo.length * 1.5)];
            System.arraycopy(keyInfo, 0, arr, 0, keyInfo.length);
            keyInfo = arr;
            arr = new int[(int) (keyInfo.length * 1.5)];
            System.arraycopy(valueInfo, 0, arr, 0, valueInfo.length);
            valueInfo = arr;
        }
        keyInfo[size * 2] = keyOffset;
        keyInfo[size * 2 + 1] = key.length;

        System.arraycopy(key, 0, keys, keyOffset, key.length);


        int valueOffset = size > 0 ? valueInfo[size * 2 - 2] + valueInfo[size * 2 - 1] : 0;
        valueInfo[size * 2] = valueOffset;
        valueInfo[size * 2 + 1] = length + fromBuffer;

        if (length < 0)
            fromBuffer += length;
        if (fromBuffer > 0)
            System.arraycopy(buffer, 0, values, valueOffset, fromBuffer);
        if (length > 0)
            System.arraycopy(value, offset, values, valueOffset + fromBuffer, length);
        size++;
    }

    public String get(String k) {
        String value;
        value = cache.get(k);
        if (value != null)
            return value;

        byte[] bytes = keyCache.get(k);
        int offset, length;
        for (int i = 0; i < size; i++) {
            length = keyInfo[i * 2 + 1];
            if (length != bytes.length)
                continue;

            offset = keyInfo[i * 2];

            int ll = offset + length;
            boolean match = true;
            for (int j = offset; j < ll && match; j++) {
                if (keys[j] != bytes[j - offset])
                    match = false;
            }
            if (match) {
                offset = valueInfo[i * 2];
                length = valueInfo[i * 2 + 1];
                value = new String(values, offset, length).trim();
                cache.put(k, value);
                return value;
            }

        }
        return null;
    }

    public void put(String key, String value) {
        int offset = size == 0 ? 0 : keyInfo[(size - 1) * 2] + keyInfo[(size - 1) * 2 + 1];
        keyInfo[size * 2] = offset;
        byte[] bytes = key.getBytes();
        keyInfo[size * 2 + 1] = bytes.length;
        System.arraycopy(bytes, 0, keys, offset, bytes.length);

        offset = size == 0 ? 0 : valueInfo[(size - 1) * 2] + valueInfo[(size - 1) * 2 + 1];
        valueInfo[size * 2] = offset;
        bytes = value.getBytes();
        valueInfo[size * 2 + 1] = bytes.length;
        System.arraycopy(bytes, 0, values, offset, bytes.length);

        cache.put(key, value);
        size++;
    }
}