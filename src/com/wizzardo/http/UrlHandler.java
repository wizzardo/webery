package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;

import java.io.IOException;

/**
 * Created by wizzardo on 10.01.15.
 */
public class UrlHandler implements Handler {
    private UrlMapping<Handler> mapping = new UrlMapping<>();

    @Override
    public Response handle(Request request, Response response) throws IOException {
        Handler handler = mapping.get(request);
        if (handler != null)
            return handler.handle(request, response);


        return response.setStatus(Status._404).setBody(request.path() + " not found");
    }

    public UrlHandler append(String url, Handler handler) {
        mapping.append(url, handler);
        return this;
    }
}
