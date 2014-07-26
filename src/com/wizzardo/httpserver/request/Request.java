package com.wizzardo.httpserver.request;

import com.wizzardo.httpserver.MultiValue;
import com.wizzardo.httpserver.HttpConnection;

import java.util.List;
import java.util.Map;

/**
 * @author: wizzardo
 * Date: 7/25/14
 */
public class Request {

    private HttpConnection connection;
    private Map<String, MultiValue> headers;
    private Map<String, MultiValue> params;
    private Method method;
    private String path;
    private String queryString;

    public static enum Method {
        GET, PUT, POST, DELETE;
    }

    public Request(HttpConnection connection, Map<String, MultiValue> headers, Map<String, MultiValue> params, String method, String path, String queryString) {
        this.connection = connection;
        this.headers = headers;
        this.params = params;
        this.method = Method.valueOf(method);
        this.path = path;
        this.queryString = queryString;
    }

    public String path() {
        return path;
    }

    public HttpConnection connection() {
        return connection;
    }

    public Map<String, MultiValue> headers() {
        return headers;
    }

    public String header(String key) {
        MultiValue value = headers.get(key);
        return value == null ? null : value.getValue();
    }

    public List<String> headers(String key) {
        MultiValue value = headers.get(key);
        return value == null ? null : value.getValues();
    }

    public Method method() {
        return method;
    }

    public String getQueryString() {
        return queryString;
    }

    public String param(String key) {
        MultiValue value = params.get(key);
        return value == null ? null : value.getValue();
    }

    public List<String> params(String key) {
        MultiValue value = params.get(key);
        return value == null ? null : value.getValues();
    }
}
