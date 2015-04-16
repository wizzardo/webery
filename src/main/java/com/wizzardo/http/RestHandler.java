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

    protected Handler get;
    protected Handler post;
    protected Handler put;
    protected Handler delete;

    private byte[] allow;

    @Override
    public Response handle(Request request, Response response) throws IOException {
        switch (request.method()) {
            case GET:
                return handle(request, response, get);
            case PUT:
                return handle(request, response, put);
            case POST:
                return handle(request, response, post);
            case DELETE:
                return handle(request, response, delete);
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
        if (allow != null)
            response.appendHeader(allow);
        return response;
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

    private void generateAllowHeader() {
        StringBuilder sb = new StringBuilder("Allow: ");
        boolean comma;

        comma = buildAllowHeaderString(sb, get, "GET", false);
        comma = buildAllowHeaderString(sb, post, "POST", comma);
        comma = buildAllowHeaderString(sb, put, "PUT", comma);
        comma = buildAllowHeaderString(sb, delete, "DELETE", comma);

        if (sb.length() > 7)
            allow = sb.append("\r\n").toString().getBytes(StandardCharsets.UTF_8);
        else
            allow = null;
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
