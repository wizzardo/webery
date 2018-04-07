package com.wizzardo.http;

import com.wizzardo.http.request.ByteTree;
import com.wizzardo.http.utils.AsciiReader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by wizzardo on 12.06.15.
 */
public abstract class HttpHeadersReader {

    protected ByteTree headersTree;
    protected byte[] buffer;
    protected int r;
    protected String tempKey;
    protected boolean waitForNewLine;
    protected Map<String, MultiValue> headers;

    protected boolean complete = false;
    protected boolean onlyCachedHeaders = false;

    public HttpHeadersReader() {
        this(new LinkedHashMap<>(16));
    }

    public HttpHeadersReader(Map<String, MultiValue> headers) {
        this(headers, null);
    }

    public HttpHeadersReader(Map<String, MultiValue> headers, ByteTree headersTree) {
        this.headers = headers != null ? headers : new LinkedHashMap<>(16);
        this.headersTree = headersTree;
    }

    public void clear() {
        if (!headers.isEmpty())
            headers = new LinkedHashMap<>(16);
        tempKey = null;
        complete = false;
        waitForNewLine = false;
        r = 0;
    }

    public boolean isOnlyCachedHeaders() {
        return onlyCachedHeaders;
    }

    public void setOnlyCachedHeaders(boolean onlyCachedHeaders) {
        this.onlyCachedHeaders = onlyCachedHeaders;
    }

    public int read(byte[] bytes) {
        return read(bytes, 0, bytes.length);
    }

    /**
     * @return new offset in given byte array to read from.
     */
    protected abstract int parseFirstLine(byte[] chars, int offset, int length);

    protected int parseHeadersWithFirstLine(byte[] chars, int offset, int length) {
        int newOffset = parseFirstLine(chars, offset, length);
        length -= (newOffset - offset);
        if (length > 0)
            return parseHeaders(chars, newOffset, length);
        else
            return -1;
    }

    protected int parseHeaders(byte[] chars, int offset, int length) {
        int l = offset + length;

        if (waitForNewLine) {
            for (int i = offset; i < l; i += 2) {
                byte ch = chars[i];
                if (ch == '\n') {
                    if (i > offset) {
                        if (chars[i - 1] == '\r') {
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
                        r--;
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
                } else if (ch == '\r' && ++i < l && chars[i] == '\n') {
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
     * -1 if headers aren't completed
     */
    public int read(byte[] bytes, int offset, int length) {
        if (complete || length == 0)
            return -1;

        return parseHeadersWithFirstLine(bytes, offset, length);
    }

    protected void put(String key, String value) {
        MultiValue hv = headers.putIfAbsent(key, new MultiValue(value));
        if (hv != null)
            hv.append(value);
    }

    protected void putIntoBuffer(byte[] bytes, int offset, int length) {
        if (length == 0)
            return;

        if (buffer == null) {
            buffer = new byte[length];

        } else if (buffer.length - r < length) {
            byte[] b = new byte[r + length];
            if (r != 0)
                System.arraycopy(buffer, 0, b, 0, r);
            buffer = b;
        }

        System.arraycopy(bytes, offset, buffer, r, length);
        r += length;
    }

    protected byte[] getCharsValue(byte[] chars, int offset, int length) {
        if (r > 0) {
            byte[] b = new byte[length + r];

            System.arraycopy(buffer, 0, b, 0, r);
            if (length > 0)
                System.arraycopy(chars, offset, b, r, length);

            r = 0;
            return b;
        }

        byte[] b = new byte[length];
        if (length > 0)
            System.arraycopy(chars, offset, b, 0, length);
        return b;
    }

    protected String getValue(byte[] chars, int offset, int length, String ifEmpty) {
        String s = getValue(chars, offset, length);
        return s.isEmpty() ? ifEmpty : s;
    }

    protected String getValue(byte[] chars, int offset, int length) {
        ByteTree.Node byteTree = headersTree != null ? headersTree.getRoot() : null;

        if (r > 0) {
            if (length < 0)
                length = 0;

            int bo = 0;
            byte[] buffer = this.buffer;
            while (bo < buffer.length && buffer[bo] <= ' ') {
                bo++;
            }
            r -= bo;

            if (byteTree != null)
                byteTree = byteTree.getNode(buffer, bo, r);
            if (byteTree != null) {
                byteTree = byteTree.getNode(chars, offset, length);
                if (byteTree != null) {
                    String value = byteTree.getValue();
                    if (value != null) {
                        r = 0;
                        return value;
                    } else if (onlyCachedHeaders) {
                        r = 0;
                        return null;
                    }
                } else if (onlyCachedHeaders) {
                    r = 0;
                    return null;
                }
            } else if (onlyCachedHeaders) {
                r = 0;
                return null;
            }

            byte[] b = new byte[length + r];
            System.arraycopy(buffer, bo, b, 0, r);
            if (length > 0)
                System.arraycopy(chars, offset, b, r, length);
            length = b.length;
            offset = 0;
            chars = b;
            r = 0;
        }

        while (length > 0 && chars[offset] <= ' ') {
            offset++;
            length--;
        }
        while (length > 0 && chars[offset + length - 1] <= ' ') {
            length--;
        }

        if (byteTree != null) {
            String value = byteTree.get(chars, offset, length);
            if (value != null || onlyCachedHeaders)
                return value;
        }
        return AsciiReader.read(chars, offset, length);
    }

    public Map<String, MultiValue> getHeaders() {
        return headers;
    }

    public boolean isComplete() {
        return complete;
    }
}
