package com.wizzardo.http.framework;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: moxa
 * Date: 7/24/13
 */
public class RequestHolder {
    public final Request request;
    public final Response response;
    private volatile Map<Object, Object> requestScope;

    public RequestHolder(Request request, Response response) {
        this.request = request;
        this.response = response;
    }

    public <T> T get(Object key) {
        if (requestScope == null)
            return null;

        return (T) requestScope.get(key);
    }

    public void put(Object key, Object value) {
        if (requestScope == null)
            requestScope = new ConcurrentHashMap<>();

        requestScope.put(key, value);
    }
}
