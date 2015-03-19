package com.wizzardo.http.filter;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.security.MD5;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wizzardo on 23.02.15.
 */
public class TokenFilter implements AuthFilter {

    protected final long HOUR = 60l * 60 * 1000;

    //secret = md5(user:password)
    //key = md5(user)
    //token = key + md5(timestamp + secret) + timestamp

    //hashes - key:secret
    protected Map<String, String> hashes = new HashMap<>();
    protected AuthFilter authFilter;

    public TokenFilter(AuthFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Override
    public boolean filter(Request request, Response response) {
        String token;
        if ((token = request.param("token")) == null)
            return authFilter.filter(request, response);

        if (token.length() <= 64)
            return returnNotAuthorized(response);

        String secret = hashes.get(token.substring(0, 32));
        if (secret == null)
            return returnNotAuthorized(response);

        String sign = token.substring(32, 64);
        String timestamp = token.substring(64);

        if (!sign.equals(sign(timestamp, secret)))
            return returnNotAuthorized(response);

        long time;
        try {
            time = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return returnNotAuthorized(response);
        }

        if (System.currentTimeMillis() > time)
            return returnNotAuthorized(response);

        return true;
    }

    @Override
    public boolean returnNotAuthorized(Response response) {
        return authFilter.returnNotAuthorized(response);
    }

    @Override
    public TokenFilter allow(String user, String password) {
        authFilter.allow(user, password);
        hashes.put(MD5.getMD5AsString(user), MD5.getMD5AsString(user + ":" + password));

        return this;
    }

    @Override
    public String getUser(Request request) {
        return authFilter.getUser(request);
    }

    public String generateToken(Request request) {
        String auth = request.header(Header.KEY_AUTHORIZATION);
        if (auth == null)
            return "";

        String key = MD5.getMD5AsString(getUser(request));
        String secret = hashes.get(key);

        long timestamp = System.currentTimeMillis() + HOUR * 12;
        return key + sign(timestamp, secret) + timestamp;
    }

    private String sign(String timestamp, String secret) {
        return MD5.getMD5AsString(timestamp + secret);
    }

    private String sign(long timestamp, String secret) {
        return sign(String.valueOf(timestamp), secret);
    }
}
