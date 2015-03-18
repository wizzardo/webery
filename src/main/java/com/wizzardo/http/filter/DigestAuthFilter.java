package com.wizzardo.http.filter;

import com.wizzardo.http.Filter;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.reflection.StringReflection;
import com.wizzardo.tools.security.MD5;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author: wizzardo
 * Date: 29.11.14
 */
public class DigestAuthFilter implements Filter {
    protected Map<String, String> userPasswords = new HashMap<>();
    protected Random random = new Random();
    protected String realm = "simple http server";

    public DigestAuthFilter() {
    }

    public DigestAuthFilter(String realm) {
        this.realm = realm;
    }

    @Override
    public boolean filter(Request request, Response response) {
        String auth = request.header(Header.KEY_AUTHORIZATION);

        if (auth == null || !auth.startsWith("Digest "))
            return returnNotAuthorized(response);

        DigestAuthData authData = new DigestAuthData(auth);
        String password = userPasswords.get(authData.username);
        if (password == null)
            return returnNotAuthorized(response);

        String ha1 = MD5.getMD5AsString(authData.username + ":" + realm + ":" + password);
        String ha2 = MD5.getMD5AsString(request.method().name() + ":" + request.path().toString());

        String resp;
        if ("auth".equals(authData.qop))
            resp = MD5.getMD5AsString(ha1 + ":" + authData.nonce + ":" + authData.nc + ":" + authData.cnonce + ":" + authData.qop + ":" + ha2);
        else
            resp = MD5.getMD5AsString(ha1 + ":" + authData.nonce + ":" + ha2);

        System.out.println("h1: " + ha1);
        System.out.println("h2: " + ha2);
        System.out.println("response: " + resp);

        if (!resp.equals(authData.response))
            return returnNotAuthorized(response);

        return true;
    }

    protected boolean returnNotAuthorized(Response response) {
        response.setStatus(Status._401);
        response.header(Header.KEY_WWW_AUTHENTICATE, "Digest realm=\"" + realm + "\", qop=\"auth\", nonce:\"" + nonce() + "\"");
        return false;
    }

    public DigestAuthFilter allow(String user, String password) {
        userPasswords.put(user, password);
        return this;
    }

    protected String nonce() {
        return bytesToHex(randomBytes(16));
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    protected String bytesToHex(byte[] bytes) {
        String str = new BigInteger(1, bytes).toString(16);
        while (str.length() < bytes.length * 2) {
            str = "0" + str;
        }
        return str;
    }

    static class DigestAuthData {
        String username;
        String realm;
        String uri;
        String qop;
        String nc;
        String cnonce;
        String nonce;
        String response;
        String opaque;

        public DigestAuthData(String auth) {
            parse(auth);
        }

        private void parse(String auth) {
            char[] chars = StringReflection.chars(auth);
            int i = 7;
            int length = chars.length;
            while (i < length) {
                int nameStart = skipSpaces(chars, i);
                int nameEnd = find(chars, i, length, '=');
                if (nameEnd == -1)
                    throw new IllegalStateException("can't parse key from string '" + auth + "'");

                int valueStart = nameEnd + 1;
                int valueEnd;
                String value;
                if (chars[valueStart] == '"') {
                    valueEnd = find(chars, valueStart + 1, length, '"');
                    valueStart++;
                    if (valueEnd == -1)
                        throw new IllegalStateException("can't parse value for key '" + new String(chars, valueStart, valueEnd - valueStart) + "', from string '" + auth + "'");
                    value = new String(chars, valueStart, valueEnd - valueStart);
                    i = valueEnd + 1;
                } else {
                    valueEnd = find(chars, valueStart, length, ',');
                    if (valueEnd == -1)
                        valueEnd = length;
                    value = new String(chars, valueStart, valueEnd - valueStart);
                    i = valueEnd;
                }

                setValue(chars, nameStart, nameEnd, value);

                if (i != length && chars[i] != ',')
                    throw new IllegalStateException("can't parse string '" + auth + "'");
                i++;
            }
        }

        private void setValue(char[] chars, int nameStart, int nameEnd, String value) {
            int l = nameEnd - nameStart;
            int i = nameStart;
            if (l == 8 && chars[i] == 'u' && chars[i + 1] == 's' && chars[i + 2] == 'e' && chars[i + 3] == 'r' && chars[i + 4] == 'n' && chars[i + 5] == 'a' && chars[i + 6] == 'm' && chars[i + 7] == 'e') {
                username = value;
            } else if (l == 5 && chars[i] == 'r' && chars[i + 1] == 'e' && chars[i + 2] == 'a' && chars[i + 3] == 'l' && chars[i + 4] == 'm') {
                realm = value;
            } else if (l == 3 && chars[i] == 'u' && chars[i + 1] == 'r' && chars[i + 2] == 'i') {
                uri = value;
            } else if (l == 3 && chars[i] == 'q' && chars[i + 1] == 'o' && chars[i + 2] == 'p') {
                qop = value;
            } else if (l == 2 && chars[i] == 'n' && chars[i + 1] == 'c') {
                nc = value;
            } else if (l == 5 && chars[i] == 'n' && chars[i + 1] == 'o' && chars[i + 2] == 'n' && chars[i + 3] == 'c' && chars[i + 4] == 'e') {
                nonce = value;
            } else if (l == 6 && chars[i] == 'c' && chars[i + 1] == 'n' && chars[i + 2] == 'o' && chars[i + 3] == 'n' && chars[i + 4] == 'c' && chars[i + 5] == 'e') {
                cnonce = value;
            } else if (l == 8 && chars[i] == 'r' && chars[i + 1] == 'e' && chars[i + 2] == 's' && chars[i + 3] == 'p' && chars[i + 4] == 'o' && chars[i + 5] == 'n' && chars[i + 6] == 's' && chars[i + 7] == 'e') {
                response = value;
            } else if (l == 6 && chars[i] == 'o' && chars[i + 1] == 'p' && chars[i + 2] == 'a' && chars[i + 3] == 'q' && chars[i + 4] == 'u' && chars[i + 5] == 'e') {
                opaque = value;
            }
        }

        private int find(char[] chars, int offset, int length, char c) {
            int i = offset;
            while (i < length) {
                char ch = chars[offset];
                if (ch == c && (i == offset || chars[i - 1] != '\\'))
                    return offset;
                offset++;
            }
            return -1;
        }

        private int skipSpaces(char[] chars, int i) {
            int l = chars.length;
            while (i < l && chars[i] <= 32)
                i++;
            return i;
        }
    }

}
