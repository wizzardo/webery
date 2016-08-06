package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.io.IOException;

public interface ErrorHandler {
    Response handle(Request request, Response response, Exception e) throws Exception;
}
