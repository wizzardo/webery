package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by wizzardo on 16.04.15.
 */
public class RestHandler implements Handler {

    protected final String name;
    protected Handler get;
    protected Handler post;
    protected Handler put;
    protected Handler delete;
    protected final Handler options = (request, response) -> provideAllowHeader(response);

    private byte[] allow;

    public RestHandler() {
        this(null);
    }

    public RestHandler(String name) {
        this.name = name;
        generateAllowHeader();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Response handle(Request request, Response response) throws IOException {
        switch (request.method()) {
            case HEAD:
            case GET:
                return handle(request, response, get);
            case PUT:
                return handle(request, response, put);
            case POST:
                return handle(request, response, post);
            case DELETE:
                return handle(request, response, delete);
            case OPTIONS:
                return handle(request, response, options);
        }
        return response.setStatus(Status._405);
    }

    protected Response handle(Request request, Response response, Handler handler) throws IOException {
        if (handler == null)
            return provideAllowHeader(response).setStatus(Status._405);
        else
            return handler.handle(request, response);
    }

    protected Response provideAllowHeader(Response response) {
        return response.appendHeader(allow);
    }

    public RestHandler setGetHandler(Handler get) {
        this.get = get;
        generateAllowHeader();
        return this;
    }

    public RestHandler setPostHandler(Handler post) {
        this.post = post;
        generateAllowHeader();
        return this;
    }

    public RestHandler setPutHandler(Handler put) {
        this.put = put;
        generateAllowHeader();
        return this;
    }

    public RestHandler setDeleteHandler(Handler delete) {
        this.delete = delete;
        generateAllowHeader();
        return this;
    }

    public RestHandler set(Request.Method method, Handler handler) {
        switch (method) {
            case GET:
                return get(handler);
            case PUT:
                return put(handler);
            case POST:
                return post(handler);
            case DELETE:
                return delete(handler);
            default:
                throw new IllegalArgumentException("Method " + method + " is not supported with custom handler");
        }
    }

    public RestHandler get(Handler get) {
        return setGetHandler(get);
    }

    public RestHandler post(Handler post) {
        return setPostHandler(post);
    }

    public RestHandler put(Handler put) {
        return setPutHandler(put);
    }

    public RestHandler delete(Handler delete) {
        return setDeleteHandler(delete);
    }

    private void generateAllowHeader() {
        StringBuilder sb = new StringBuilder("Allow: ");
        boolean comma;

        comma = buildAllowHeaderString(sb, get, "GET", false);
        comma = buildAllowHeaderString(sb, get, "HEAD", comma);
        comma = buildAllowHeaderString(sb, post, "POST", comma);
        comma = buildAllowHeaderString(sb, put, "PUT", comma);
        comma = buildAllowHeaderString(sb, delete, "DELETE", comma);
        comma = buildAllowHeaderString(sb, options, "OPTIONS", comma);

        allow = sb.append("\r\n").toString().getBytes(StandardCharsets.UTF_8);
    }

    private boolean buildAllowHeaderString(StringBuilder sb, Handler handler, String name, boolean comma) {
        if (handler != null) {
            if (comma)
                sb.append(", ");
            else
                comma = true;
            sb.append(name);
        }
        return comma;
    }
}
