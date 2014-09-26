package com.wizzardo.http;


import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

/**
 * @author: moxa
 * Date: 11/3/12
 */
public class Filter {

    public boolean before(Request request, Response response) {
        return true;
    }

    public boolean after(Request request, Response response) {
        return true;
    }

}
