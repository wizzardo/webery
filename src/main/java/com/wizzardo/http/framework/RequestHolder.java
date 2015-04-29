package com.wizzardo.http.framework;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

/**
 * @author: moxa
 * Date: 7/24/13
 */
public class RequestHolder {
    public final Request request;
    public final Response response;

    public RequestHolder(Request request, Response response) {
        this.request = request;
        this.response = response;
    }
}
