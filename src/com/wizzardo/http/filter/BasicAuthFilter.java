package com.wizzardo.http.filter;

import com.wizzardo.http.Filter;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.security.Base64;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: wizzardo
 * Date: 29.11.14
 */
public class BasicAuthFilter implements Filter {
    protected Set<String> userPasswords = new HashSet<>();

    @Override
    public boolean filter(Request request, Response response) {
        String s = request.header(Header.KEY_AUTHORIZATION);
        if (s != null && s.startsWith("Basic ")) {
            s = new String(Base64.decode(s.substring(6)));
            if (userPasswords.contains(s))
                return true;
        }

        response.setStatus(Status._401);
        response.header(Header.KEY_WWW_AUTHENTICATE, "Basic realm=\"simple http server\"");
        return false;
    }

    public BasicAuthFilter allow(String user, String password) {
        userPasswords.add(user + ":" + password);
        return this;
    }
}
