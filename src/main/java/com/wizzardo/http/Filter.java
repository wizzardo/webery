package com.wizzardo.http;


import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

/**
 * @author: moxa
 * Date: 11/3/12
 */
public interface Filter {

    public boolean filter(Request request, Response response);

}
