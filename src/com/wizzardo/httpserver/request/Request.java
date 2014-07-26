package com.wizzardo.httpserver.request;

import com.wizzardo.httpserver.HeaderValue;
import com.wizzardo.httpserver.HttpConnection;

import java.util.List;
import java.util.Map;

/**
 * @author: wizzardo
 * Date: 7/25/14
 */
public class Request {

    private HttpConnection connection;
    private Map<String, HeaderValue> headers;
    private Method method;
    private String path;

    public static enum Method {
        GET, PUT, POST, DELETE;
    }

    public Request(HttpConnection connection, Map<String, HeaderValue> headers, String method, String path) {
        this.connection = connection;
        this.headers = headers;
        this.method = Method.valueOf(method);
        this.path = path;
    }

    public HttpConnection connection() {
        return connection;
    }

    public Map<String, HeaderValue> headers() {
        return headers;
    }

    public String header(String key) {
        HeaderValue value = headers.get(key);
        return value == null ? null : value.getValue();
    }

    public List<String> headers(String key) {
        HeaderValue value = headers.get(key);
        return value == null ? null : value.getValues();
    }

    public Method method() {
        return method;
    }
}
