package com.wizzardo.http.request;

import com.wizzardo.http.HttpException;
import com.wizzardo.http.HttpHeadersReader;
import com.wizzardo.http.MultiValue;
import com.wizzardo.http.mapping.Path;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.response.Status;
import com.wizzardo.http.utils.AsciiReader;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public class RequestReader extends HttpHeadersReader {

    protected Parameters params;
    protected String method;
    protected Path path;
    protected String queryString;
    protected String protocol;

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path.toString();
    }

    public String getProtocol() {
        return protocol;
    }

    public RequestReader() {
        this(null, null);
    }

    public RequestReader(Map<String, MultiValue> headers) {
        this(headers, null);
    }

    public RequestReader(Map<String, MultiValue> headers, Parameters params) {
        super(headers);
        if (params == null)
            params = new Parameters();

        this.params = params;
    }

    public void clear() {
        super.clear();
        params = new Parameters();
        method = null;
        queryString = null;
        protocol = null;
        path = null;
    }

    public Request fillRequest(Request request) {
        request.headers = headers;
        request.params = params;
        try {
            request.method = Request.Method.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new HttpException(e, Status._501);
        }
        request.path = path;
        request.queryString = queryString;
        request.protocol = protocol;

        if (request.contentLength() > 0
                && request.contentLength() < POST_BODY_SIMPLE_LIMIT
                && !request.isMultipart())
            request.body = new SimpleRequestBody((int) request.contentLength());
        return request;
    }


    protected void parsePath(byte[] chars, int offset, int length) {
        chars = getCharsValue(chars, offset, length);
        if (chars.length == 0)
            return;
        path = Path.parse(chars, 0, chars.length, UrlMapping.SEGMENT_CACHE);
    }

    protected void parseQueryString(byte[] chars, int offset, int length) {
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

    protected void parseParameters(byte[] chars, int offset, int length) {
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

    protected void putParameter(String key, String value) {
        MultiValue multiValue = params.putIfAbsent(key, new MultiValue(value));
        if (multiValue != null)
            multiValue.append(value);
    }

    protected String decodeParameter(String s) {
        try {
            return URLDecoder.decode(s, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected int parseFirstLine(byte[] chars, int offset, int length) {
        if (protocol != null)
            return offset;

        int l = offset + length;
        for (int i = offset; i < l; i++) {
            byte b = chars[i];
            if (b == ' ' || b == '?') {
                if (method == null) {
                    method = getValue(chars, offset, i - offset);
                    if (method.isEmpty())
                        method = null;

                    i++;
                    return parseFirstLine(chars, i, length - (i - offset));
                } else if (path == null) {
                    parsePath(chars, offset, i - offset);
                    i++;
                    return parseFirstLine(chars, i, length - (i - offset));
                } else if (queryString == null) {
                    parseQueryString(chars, offset, i - offset);
                    i++;
                    return parseFirstLine(chars, i, length - (i - offset));
                }

            } else if (b == '\n') {
                protocol = getValue(chars, offset, i - offset);

                i++;
                return i;
            }
        }
        return offset;
    }
}