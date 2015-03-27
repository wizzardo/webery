package com.wizzardo.http.request;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.MultiValue;
import com.wizzardo.http.mapping.Path;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.utils.AsciiReader;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public class RequestReader {
    private static final int POST_BODY_SIMPLE_LIMIT = 2 * 1024 * 1024;
    private static final ByteTree headersTree = new ByteTree();

    static {
        for (Header header : Header.values()) {
            headersTree.appendIgnoreCase(header.value);
        }
        for (Request.Method method : Request.Method.values()) {
            headersTree.appendIgnoreCase(method.name());
        }
        headersTree.appendIgnoreCase("gzip,deflate,sdch");
        headersTree.appendIgnoreCase("en-US,en;q=0.8,ru;q=0.6");
        headersTree.append(HttpConnection.HTTP_1_0);
        headersTree.append(HttpConnection.HTTP_1_1);
    }

    protected Map<String, MultiValue> headers;
    protected Map<String, MultiValue> params;
    protected String method;
    protected Path path;
    protected String queryString;
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
        return path.toString();
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

    public Request fillRequest(Request request) {
        request.headers = headers;
        request.params = params;
        request.method = Request.Method.valueOf(method);
        request.path = path;
        request.queryString = queryString;
        request.protocol = protocol;

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
        chars = getCharsValue(chars, offset, length);
        if (chars.length == 0)
            return;
        path = Path.parse(chars, 0, chars.length, UrlMapping.SEGMENT_CACHE);
    }

    private void parseQueryString(byte[] chars, int offset, int length) {
        chars = getCharsValue(chars, offset, length);
        length = chars.length;
        offset = 0;
        if (length == 1 && chars[offset] == '?') {
            queryString = "";
            return;
        }
        if (length > 0) {
            queryString = AsciiReader.read(chars, offset, length);
            parseParameters(chars, offset, length);
        } else
            queryString = "";
    }

    void parseParameters(byte[] chars, int offset, int length) {
        int from = offset;
        int to = offset + length;
        String key = null;
        boolean isKey = true;
        for (int i = from; i < to; i++) {
            if (isKey) {
                if (chars[i] == '=') {
                    key = decodeParameter(AsciiReader.read(chars, from, i - from));
                    from = i + 1;
                    isKey = false;
                }
                if (chars[i] == '&' && from != i - 1) {
                    key = decodeParameter(AsciiReader.read(chars, from, i - from));
                    from = i + 1;
                }
            } else {
                if (chars[i] == '&') {
                    String value = decodeParameter(AsciiReader.read(chars, from, i - from));
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
                byte b = chars[i];
                if (b == ' ' || b == '?') {
                    if (method == null) {
                        method = getValue(chars, offset, i - offset);
                        if (method.isEmpty())
                            method = null;

                        i++;
                        return parseHeaders(chars, i, length - (i - offset));
                    } else if (path == null) {
                        parsePath(chars, offset, i - offset);
                        i++;
                        return parseHeaders(chars, i, length - (i - offset));
                    } else if (queryString == null) {
                        parseQueryString(chars, offset, i - offset);
                        i++;
                        return parseHeaders(chars, i, length - (i - offset));
                    }

                } else if (b == '\n') {
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

    private byte[] getCharsValue(byte[] chars, int offset, int length) {
        if (r > 0) {
            int bo = 0;
            while (bo < buffer.length && buffer[bo] <= ' ') {
                bo++;
            }
            r -= bo;

            byte[] b = new byte[length + r];

            System.arraycopy(buffer, bo, b, 0, r);
            if (length > 0)
                System.arraycopy(chars, offset, b, r, length);

            r = 0;
            return b;
        }

        while (length > 0 && chars[offset] <= ' ') {
            offset++;
            length--;
        }
        while (length > 0 && chars[offset + length - 1] <= ' ') {
            length--;
        }

        byte[] b = new byte[length];
        if (length > 0)
            System.arraycopy(chars, offset, b, 0, length);
        return b;
    }

    private String getValue(byte[] chars, int offset, int length) {
        ByteTree.Node byteTree = headersTree.getRoot();

        if (r > 0) {
            if (length < 0)
                length = 0;

            int bo = 0;
            while (bo < buffer.length && buffer[bo] <= ' ') {
                bo++;
            }
            r -= bo;

            byteTree = byteTree.getNode(buffer, bo, r);
            if (byteTree != null) {
                byteTree = byteTree.getNode(chars, offset, length);
                if (byteTree != null) {
                    String value = byteTree.getValue();
                    if (value != null) {
                        r = 0;
                        return value;
                    }
                }
            }

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

        while (length > 0 && chars[offset] <= ' ') {
            offset++;
            length--;
        }
        while (length > 0 && chars[offset + length - 1] <= ' ') {
            length--;
        }

        if (byteTree != null) {
            String value = byteTree.get(chars, offset, length);
            if (value != null)
                return value;
        }
        return AsciiReader.read(chars, offset, length);
    }

    public Map<String, MultiValue> getHeaders() {
        return headers;
    }

}