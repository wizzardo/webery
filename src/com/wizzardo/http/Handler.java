package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

/**
 * @author: wizzardo
 * Date: 07.09.14
 */
public interface Handler {

    public Response handle(Request request);
}
