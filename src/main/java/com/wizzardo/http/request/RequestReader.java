package com.wizzardo.http.request;

import com.wizzardo.http.HttpException;
import com.wizzardo.http.HttpHeadersReader;
import com.wizzardo.http.MultiValue;
import com.wizzardo.http.mapping.Path;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.response.Status;
import com.wizzardo.http.utils.AsciiReader;
import com.wizzardo.http.utils.PercentEncoding;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public class RequestReader extends HttpHeadersReader {

    protected Parameters params;
    protected String method;
    protected PathHolder pathHolder = new PathHolder();
    protected String queryString;
    protected String protocol;

    public RequestReader(Map<String, MultiValue<String>> headers, ByteTree cacheTree, Parameters parameters) {
        super(headers, cacheTree);
        this.params = parameters != null ? parameters : new Parameters();
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return pathHolder.path.toString();
    }

    public String getProtocol() {
        return protocol;
    }

    public RequestReader() {
        this(null, null, null);
    }

    public RequestReader(Map<String, MultiValue<String>> headers) {
        this(headers, null, null);
    }

    public RequestReader(Map<String, MultiValue<String>> headers, Parameters params) {
        this(headers, null, params);
    }

    static class PathHolder {
        Path path = new Path(8);
        boolean parsed = false;

        public void clear() {
            parsed = false;
            path.clear();
        }
    }

    public void clear() {
        super.clear();
        if (!params.isEmpty())
            params = new Parameters();

        method = null;
        queryString = null;
        protocol = null;
        pathHolder.clear();
    }

    public Request fillRequest(Request request) {
        request.headers = headers;
        request.params = params;
        try {
            request.method = Request.Method.valueOf(method);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new HttpException(e, Status._501);
        }
        request.path = pathHolder.path;
        request.queryString = queryString;
        request.protocol = protocol;

        if (request.contentLength() > 0
                && request.contentLength() < request.connection().getServer().getPostBodyLimit()
                && !request.isMultipart())
            request.body = new SimpleRequestBody((int) request.contentLength());
        return request;
    }


    protected void parsePath(byte[] chars, int offset, int length) {
        if (r != 0) {
            chars = getCharsValue(chars, offset, length);
            Path.parse(chars, 0, chars.length, UrlMapping.SEGMENT_CACHE, pathHolder.path);
        } else {
            if (length == 0)
                return;
            Path.parse(chars, offset, length + offset, UrlMapping.SEGMENT_CACHE, pathHolder.path);
        }
        pathHolder.parsed = true;
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
                    key = decode(chars, from, i);
                    from = i + 1;
                    isKey = false;
                }
                if (chars[i] == '&' && from != i - 1) {
                    key = decode(chars, from, i);
                    from = i + 1;
                }
            } else {
                if (chars[i] == '&') {
                    String value = decode(chars, from, i);
                    from = i + 1;
                    isKey = true;

                    putParameter(key, value);
                    key = null;
                }
            }
        }
        if (key != null) {
            String value = decode(chars, from, to);
            putParameter(key, value);
        } else if (from < to) {
            key = decode(chars, from, to);
            putParameter(key, "");
        }
    }

    private String decode(byte[] chars, int from, int i) {
        return new String(chars, from, PercentEncoding.decode(chars, from, i), StandardCharsets.UTF_8);
    }

    protected void putParameter(String key, String value) {
        params.computeIfAbsent(key, s -> new MultiValue()).append(value);
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
                } else if (!pathHolder.parsed) {
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