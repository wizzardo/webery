package com.wizzardo.http.filter;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.security.Base64;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * @author: wizzardo
 * Date: 29.11.14
 */
public class BasicAuthFilter implements AuthFilter {
    protected Set<String> userPasswords = new HashSet<>();

    @Override
    public boolean filter(Request request, Response response) {
        String s = request.header(Header.KEY_AUTHORIZATION);
        if (s != null && userPasswords.contains(s))
            return true;

        return returnNotAuthorized(response);
    }

    public boolean returnNotAuthorized(Response response) {
        response.setStatus(Status._401);
        response.body(Status._401.message);
        response.header(Header.KEY_WWW_AUTHENTICATE, "Basic realm=\"simple http server\"");
        return false;
    }

    public BasicAuthFilter allow(String user, String password) {
        userPasswords.add(headerValue(user, password));
        return this;
    }

    @Override
    public String getUser(Request request) {
        String header = request.header(Header.KEY_AUTHORIZATION);
        if (header == null)
            return null;

        header = new String(Base64.decodeFast(header.substring(6)), StandardCharsets.UTF_8);
        int i = header.indexOf(':');
        if (i == -1)
            return null;

        return header.substring(0, i);
    }

    protected String encode(String user, String password) {
        return Base64.encodeToString((user + ":" + password).getBytes());
    }

    protected String headerValue(String user, String password) {
        return "Basic " + encode(user, password);
    }
}
