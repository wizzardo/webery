package com.wizzardo.http.filter;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.io.BytesTools;
import com.wizzardo.tools.security.Base64;
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
    protected Map<BytesHolder, BytesHolder> hashes = new HashMap<>();
    protected AuthFilter authFilter;

    public TokenFilter(AuthFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Override
    public boolean filter(Request request, Response response) {
        String token;
        byte[] data;
        try {
            if ((token = request.param("token")) == null || (data = Base64.decodeFast(token, true)).length < 40)
                return authFilter.filter(request, response);
        } catch (Exception ignored) {
            return authFilter.filter(request, response);
        }

        BytesHolder secret = hashes.get(new BytesHolder(data, 0, 16));
        if (secret == null)
            return returnNotAuthorized(response);

        MD5.create().update(data, 32, 8).update(secret.bytes).asBytes(data, 0);
        if (!BytesHolder.equals(data, 0, 16, data, 16, 16))
            return returnNotAuthorized(response);

        long time = BytesTools.toLong(data, 32);
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
        hashes.put(new BytesHolder(MD5.create().update(user).asBytes()), new BytesHolder(MD5.create().update(user + ":" + password).asBytes()));

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
        byte[] token = new byte[40]; // 16+16+8
        MD5.create().update(getUser(request)).asBytes(token, 0); // key
        BytesHolder secret = hashes.get(new BytesHolder(token, 0, 16));

        long timestamp = System.currentTimeMillis() + HOUR * 12;
        BytesTools.toBytes(timestamp, token, 32, 8);
        MD5.create().update(token, 32, 8).update(secret.bytes).asBytes(token, 16);
        return Base64.encodeToString(token, false, true);
    }

    private String sign(String timestamp, String secret) {
        return MD5.create().update(timestamp + secret).asString();
    }

    private String sign(long timestamp, String secret) {
        return sign(String.valueOf(timestamp), secret);
    }

    static class BytesHolder {
        final byte[] bytes;
        final int offset;
        final int length;
        int hash;

        BytesHolder(byte[] bytes, int offset, int length) {
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
        }

        BytesHolder(byte[] bytes) {
            this(bytes, 0, bytes.length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BytesHolder that = (BytesHolder) o;
            return equals(bytes, offset, length, that.bytes, that.offset, that.length);
        }

        static boolean equals(byte[] b1, int o1, int l1, byte[] b2, int o2, int l2) {
            if (l1 != l2)
                return false;

            for (int i = 0; i < l1; i++) {
                if (b1[o1 + i] != b2[o2 + i])
                    return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            if (hash != 0)
                return hash;

            int result = 1;
            byte[] b = bytes;
            for (int i = offset; i < length - offset; i++) {
                byte element = b[i];
                result = 31 * result + element;
            }
            hash = result;
            return result;
        }
    }

}
