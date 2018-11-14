package com.wizzardo.http;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.collections.CollectionTools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.wizzardo.http.request.Request.Method.*;

/**
 * Created by wizzardo on 16.04.15.
 */
public class RestHandler implements Handler {

    protected final String name;
    protected Handler get;
    protected Handler post;
    protected Handler put;
    protected Handler delete;
    protected final Handler options = (request, response) -> response.appendHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(0));

    private byte[] allow;
    private byte[] accessControlAllowMethods;
    private byte[] allowHeaders = "Access-Control-Allow-Headers: *\r\n".getBytes();
    private byte[] allowCredentials = "Access-Control-Allow-Credentials: true\r\n".getBytes();
    private byte[] maxAge = "Access-Control-Max-Age: 1800\r\n".getBytes();

    public RestHandler() {
        this(null);
    }

    public RestHandler(String name) {
        this.name = name;
        generateAllowHeader();
        generateAccessControlAllowMethodsHeader();
    }

    public RestHandler allowHeaders(String... headers) {
        allowHeaders = ("Access-Control-Allow-Headers: " + CollectionTools.join(Arrays.asList(headers), ",") + "\r\n").getBytes();
        return this;
    }

    public RestHandler allowCredentials(boolean allow) {
        allowCredentials = ("Access-Control-Allow-Credentials: " + allow + "\r\n").getBytes();
        return this;
    }

    public RestHandler setMaxAge(int maxAge) {
        this.maxAge = ("Access-Control-Max-Age: " + maxAge + "\r\n").getBytes();
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Response handle(Request request, Response response) throws IOException {
        Request.Method i = request.method();
        if (i == GET) {
            return handle(request, response, get);
        } else if (i == POST) {
            return handle(request, response, post);
        } else if (i == OPTIONS) {
            return handle(request, response, options);
        } else if (i == PUT) {
            return handle(request, response, put);
        } else if (i == HEAD) {
            return handle(request, response, get);
        } else if (i == DELETE) {
            return handle(request, response, delete);
        }
        return response.setStatus(Status._405);
    }

    protected Response handle(Request request, Response response, Handler handler) throws IOException {
        provideAllowHeader(request, response);
        if (handler == null)
            return response.setStatus(Status._405).appendHeader(Header.KEY_CONTENT_LENGTH, String.valueOf(0));
        else
            return handler.handle(request, response);
    }

    protected Response provideAllowHeader(Request request, Response response) {
        return response
                .appendHeader(allowCredentials)
                .appendHeader(allowHeaders)
                .appendHeader("Access-Control-Allow-Origin", request.header(Header.KEY_ORIGIN, "*"))
                .appendHeader(maxAge)
                .appendHeader(accessControlAllowMethods)
                .appendHeader(allow);
    }

    public RestHandler setGetHandler(Handler get) {
        this.get = get;
        generateAllowHeader();
        generateAccessControlAllowMethodsHeader();
        return this;
    }

    public RestHandler setPostHandler(Handler post) {
        this.post = post;
        generateAllowHeader();
        generateAccessControlAllowMethodsHeader();
        return this;
    }

    public RestHandler setPutHandler(Handler put) {
        this.put = put;
        generateAllowHeader();
        generateAccessControlAllowMethodsHeader();
        return this;
    }

    public RestHandler setDeleteHandler(Handler delete) {
        this.delete = delete;
        generateAllowHeader();
        generateAccessControlAllowMethodsHeader();
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

    private void generateAccessControlAllowMethodsHeader() {
        StringBuilder sb = new StringBuilder("Access-Control-Allow-Methods: ");
        boolean comma;

        comma = buildAllowHeaderString(sb, get, "GET", false);
        comma = buildAllowHeaderString(sb, get, "HEAD", comma);
        comma = buildAllowHeaderString(sb, post, "POST", comma);
        comma = buildAllowHeaderString(sb, put, "PUT", comma);
        comma = buildAllowHeaderString(sb, delete, "DELETE", comma);
        comma = buildAllowHeaderString(sb, options, "OPTIONS", comma);

        accessControlAllowMethods = sb.append("\r\n").toString().getBytes(StandardCharsets.UTF_8);
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
