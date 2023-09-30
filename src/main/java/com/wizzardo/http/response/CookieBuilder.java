package com.wizzardo.http.response;

import com.wizzardo.http.HttpDateFormatterHolder;

import java.util.Date;

/**
 * @author: wizzardo
 * Date: 01.12.14
 */
public class CookieBuilder {

    private StringBuilder sb = new StringBuilder();

    public CookieBuilder(String key, String value) {
        sb.append(key).append('=').append(value);
    }

    public CookieBuilder expires(long date) {
        return expires(new Date(date));
    }

    public CookieBuilder expires(Date date) {
        sb.append(";Expires=").append(HttpDateFormatterHolder.get().format(date));
        return this;
    }

    public CookieBuilder path(String path) {
        sb.append(";Path=").append(path);
        return this;
    }

    public CookieBuilder domain(String domain) {
        sb.append(";Domain=").append(domain);
        return this;
    }

    public CookieBuilder maxAge(int seconds) {
        if (seconds >= 0)
            sb.append(";Max-Age=").append(seconds);
        return this;
    }

    public CookieBuilder httpOnly() {
        sb.append(";HttpOnly");
        return this;
    }

    public CookieBuilder secure() {
        sb.append(";Secure");
        return this;
    }

    public enum SameSite {
        Lax, None, Strict
    }

    public CookieBuilder sameSite(SameSite value) {
        if (value == null)
            throw new NullPointerException("SameSite value should not be null");

        sb.append(";SameSite=").append(value);
        return this;
    }

    public String build() {
        return sb.toString();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
