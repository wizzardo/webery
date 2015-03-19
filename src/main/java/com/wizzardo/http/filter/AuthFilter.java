package com.wizzardo.http.filter;

import com.wizzardo.http.Filter;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

/**
 * Created by wizzardo on 19.03.15.
 */
public interface AuthFilter extends Filter {

    boolean returnNotAuthorized(Response response);

    AuthFilter allow(String user, String password);

    String getUser(Request request);
}
