package com.wizzardo.http.framework;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHolder {
    public final Request request;
    public final Response response;
    public final long startNanoTime;
    private volatile Map<Object, Object> requestScope;

    public RequestHolder(Request request, Response response) {
        this.request = request;
        this.response = response;
        startNanoTime = System.nanoTime();
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

    public long getExecutionTimeUntilNow() {
        long time = System.nanoTime() - startNanoTime;
        return time < 0 ? -1 : time;
    }
}
