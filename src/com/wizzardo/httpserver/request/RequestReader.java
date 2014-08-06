package com.wizzardo.httpserver.request;

import com.wizzardo.httpserver.HttpConnection;
import com.wizzardo.httpserver.MultiValue;
import sun.misc.Unsafe;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public class RequestReader {
    private static final int POST_BODY_SIMPLE_LIMIT = 2 * 1024 * 1024;

    protected Map<String, MultiValue> headers;
    protected Map<String, MultiValue> params;
    protected String method;
    protected String path;
    protected String queryString = "";
    protected String protocol;

    private byte[] buffer;
    private int r;
    private String tempKey;
    private boolean waitForNewLine;

    protected boolean complete = false;

    public RequestReader() {
        this(null);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public RequestReader(Map<String, MultiValue> headers) {
        this(null, null);
    }

    public RequestReader(Map<String, MultiValue> headers, Map<String, MultiValue> params) {
        if (headers == null)
            headers = new LinkedHashMap<String, MultiValue>(175);
        if (params == null)
            params = new LinkedHashMap<String, MultiValue>();

        this.headers = headers;
        this.params = params;
    }

    public Request createRequest(HttpConnection connection) {
        Request request = new Request(connection, headers, params, method, path, queryString);
        if (request.contentLength() > 0
                && request.contentLength() < POST_BODY_SIMPLE_LIMIT
                && !request.isMultipart())
            request.body = new SimpleRequestBody((int) request.contentLength());
        return request;
    }

    public int read(byte[] bytes) {
        return read(bytes, 0, bytes.length);
    }

    private void parsePath(byte[] chars, int offset, int length) {
        int from = offset;
        int to = offset + length;
        for (int i = from; i < to; i++) {
            if (chars[i] == '?') {
                path = getValue(chars, from, i - from);
                from = i + 1;
                break;
            }
        }
        if (path == null) {
            path = getValue(chars, from, length);
            return;
        }

        queryString = getValue(chars, from, to - from);
        parseParameters(chars, from, to - from);
    }

    void parseParameters(byte[] chars, int offset, int length) {
        int from = offset;
        int to = offset + length;
        String key = null;
        boolean isKey = true;
        for (int i = from; i < to; i++) {
            if (isKey) {
                if (chars[i] == '=') {
                    key = decodeParameter(getValue(chars, from, i - from));
                    from = i + 1;
                    isKey = false;
                }
                if (chars[i] == '&' && from != i - 1) {
                    key = decodeParameter(getValue(chars, from, i - from));
                    from = i + 1;
                }
            } else {
                if (chars[i] == '&') {
                    String value = decodeParameter(getValue(chars, from, i - from));
                    from = i + 1;
                    isKey = true;

                    putParameter(key, value);
                    key = null;
                }
            }
        }
        if (key != null) {
            String value = decodeParameter(getValue(chars, from, to - from));
            putParameter(key, value);
        } else if (from < to) {
            key = decodeParameter(getValue(chars, from, to - from));
            putParameter(key, "");
        }
    }

    private void putParameter(String key, String value) {
        MultiValue multiValue = params.putIfAbsent(key, new MultiValue(value));
        if (multiValue != null)
            multiValue.append(value);
    }

    private String decodeParameter(String s) {
        try {
            return URLDecoder.decode(s, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int parseHeaders(byte[] chars, int offset, int length) {

        int l = offset + length;
        if (protocol == null) {
            for (int i = offset; i < l; i++) {
                if (chars[i] == ' ') {
                    if (method == null)
                        method = getValue(chars, offset, i - offset);
                    else if (path == null) {
                        parsePath(chars, offset, i - offset);
                    }

                    i++;

                    return parseHeaders(chars, i, length - (i - offset));
                } else if (chars[i] == '\n') {
                    protocol = getValue(chars, offset, i - offset);
                    i++;

                    return parseHeaders(chars, i, length - (i - offset));
                }
            }
        }

        if (waitForNewLine) {
            for (int i = offset; i < l; i += 2) {
                byte ch = chars[i];
                if (ch == '\n') {
                    if (i > offset) {
                        if (chars[i - 1] == 13) {  // \r
                            waitForNewLine = false;
                            if (tempKey != null) {
                                put(tempKey, getValue(chars, offset, i - offset - 1));
//                                    headers.put(tempKey, getValue(s, offset, i - offset - 1));
//                                offset++;
                                tempKey = null;
                            }
//                            r = 0;

                            i++;
                            return parseHeaders(chars, i, length - (i - offset));
                        }
                    } else if (i == offset && r > 0 && buffer[r - 1] == 13) {
                        waitForNewLine = false;
                        if (tempKey != null) {
                            put(tempKey, getValue(chars, offset, i - offset - 1));
//                                headers.put(tempKey, getValue(s, offset, i - offset - 1));
//                            offset++;
                            tempKey = null;
                        }
//                        r = 0;

                        i++;
                        return parseHeaders(chars, i, length - (i - offset));
                    }
                } else if (ch == 13 && ++i < l && chars[i] == 10) {
                    waitForNewLine = false;
                    if (tempKey != null) {
                        put(tempKey, getValue(chars, offset, i - offset - 1));
//                            headers.put(tempKey, getValue( offset, i - offset - 1));
//                        offset++;
                        tempKey = null;
                    }
//                    r = 0;

                    i++;
                    return parseHeaders(chars, i, length - (i - offset));
                }
            }

            putIntoBuffer(chars, offset, length);
            return -1;
        }


        if (length >= 2) {
            if (r == 1 && buffer[0] == '\r' && chars[offset] == '\n') {
                complete = true;
                return offset + 1;
            }
            if (chars[offset] == '\r' && chars[offset + 1] == '\n') {
                complete = true;
                return offset + 2;
            }
        } else if (r == 1 && buffer[0] == '\r' && length == 1 && chars[offset] == '\n') {
            complete = true;
            return offset + 1;
        }


        for (int i = offset; i < l; i++) {
            byte ch = chars[i];
            if (ch == ':') {
//                    tempKey = getValue(bytes, offset, i - offset);
                tempKey = getValue(chars, offset, i - offset);
//                    tempKey = getValue(s, offset, i - offset);
                waitForNewLine = true;

                i++;
                return parseHeaders(chars, i, length - (i - offset));
            }
        }

        putIntoBuffer(chars, offset, length);

        return -1;
    }


    /**
     * @return int offset in given byte array to request body.
     *         -1 if headers aren't completed
     */
    public int read(byte[] bytes, int offset, int length) {
        if (complete || length == 0)
            return -1;

        return parseHeaders(bytes, offset, length);
    }

    private void put(String key, String value) {
        MultiValue hv = headers.putIfAbsent(key, new MultiValue(value));
        if (hv != null)
            hv.append(value);
    }

    private void putIntoBuffer(byte[] bytes, int offset, int length) {
        if (buffer == null) {
            buffer = new byte[length];

        } else if (buffer.length - r < length) {
            byte[] b = new byte[r + length];
            System.arraycopy(buffer, 0, b, 0, r);
            buffer = b;
        }

        System.arraycopy(bytes, offset, buffer, r, length);
        r += length;
    }

    private String getValue(byte[] chars, int offset, int length) {
        if (r > 0) {
            int bo = 0;
            while (bo < buffer.length && buffer[bo] <= ' ') {
                bo++;
            }
            r -= bo;
            byte[] b = new byte[length + r];
            if (length < 0)
                r += length;
            System.arraycopy(buffer, bo, b, 0, r);
            if (length > 0)
                System.arraycopy(chars, offset, b, r, length);
            length = b.length;
            offset = 0;
            chars = b;
            r = 0;
        }

        while (chars[offset] <= ' ') {
            offset++;
            length--;
        }
        while (chars[offset + length - 1] <= ' ') {
            length--;
        }
        return AsciiReader.read(chars, offset, length);
    }

    public Map<String, MultiValue> getHeaders() {
        return headers;
    }

    private static class StringReflection {
        static Unsafe unsafe;
        static long array;
        static long offset;
        static long count;
        static long hash;

        static {
            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                unsafe = (Unsafe) f.get(null);

                Field array = String.class.getDeclaredField("value");
//                array.setAccessible(true);
                StringReflection.array = unsafe.objectFieldOffset(array);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            try {
                Field hash = String.class.getDeclaredField("hash");
//                hash.setAccessible(true);
                StringReflection.hash = unsafe.objectFieldOffset(hash);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            try {
                Field offset = String.class.getDeclaredField("offset");
//                offset.setAccessible(true);
                StringReflection.offset = unsafe.objectFieldOffset(offset);
            } catch (NoSuchFieldException ignored) {
            }
            try {
                Field count = String.class.getDeclaredField("count");
//                count.setAccessible(true);
                StringReflection.count = unsafe.objectFieldOffset(count);
            } catch (NoSuchFieldException ignored) {
            }
        }

        static String createString(char[] chars) {
            String s = new String();

            unsafe.putObject(s, StringReflection.array, chars);
            if (count != 0)
                unsafe.putInt(s, StringReflection.count, chars.length);
            return s;
        }

        static String createString(char[] chars, int hash) {
            String s = new String();
            unsafe.putObject(s, StringReflection.array, chars);

            if (hash != 0)
                unsafe.putInt(s, StringReflection.hash, hash);

            if (count != 0)
                unsafe.putInt(s, StringReflection.count, chars.length);
            return s;
        }

    }

    public static class AsciiReader {

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
    }

}