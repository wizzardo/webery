package com.wizzardo.http.filter;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.security.Base64;
import com.wizzardo.tools.security.MD5;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wizzardo on 23.02.15.
 */
public class BaseAuthTokenFilter extends BasicAuthFilter {

    protected final long HOUR = 60l * 60 * 1000;

    //innerHash = md5("Basic " + baseAuth)
    //token = base64(md5("md5: " + innerHash) + md5(timestamp + innerHash) + timestamp)
    protected Map<String, String> hashesOuter = new HashMap<>();
    protected Map<String, String> hashesInner = new HashMap<>();

    @Override
    public boolean filter(Request request, Response response) {
        String token;
        if ((token = request.param("token")) == null)
            return super.filter(request, response);

        if (token.length() <= 64)
            return returnNotAuthorized(response);

        String decoded = new String(Base64.decode(token));
        String authHash = decoded.substring(0, 32);

        String innerHash = hashesOuter.get(authHash);
        if (innerHash == null)
            return returnNotAuthorized(response);

        String sign = decoded.substring(32, 64);
        String timestamp = decoded.substring(64);

        if (!sign.equals(MD5.getMD5AsString((timestamp + innerHash).getBytes())))
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
    public BasicAuthFilter allow(String user, String password) {
        super.allow(user, password);
        String inner = MD5.getMD5AsString(headerValue(user, password));
        String outer = MD5.getMD5AsString("md5: " + inner);
        hashesOuter.put(outer, inner);
        hashesInner.put(inner, outer);

        return this;
    }

    public String generateToken(Request request) {
        String auth = request.header(Header.KEY_AUTHORIZATION);
        if (auth == null)
            return "";

        String innerHash = MD5.getMD5AsString(auth);
        String outerHash = hashesInner.get(innerHash);

        long timestamp = System.currentTimeMillis() + HOUR * 12;
        String sign = MD5.getMD5AsString((timestamp + innerHash).getBytes());

        return Base64.encodeToString((outerHash + sign + timestamp).getBytes());
    }
}
