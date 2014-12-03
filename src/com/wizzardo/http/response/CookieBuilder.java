package com.wizzardo.http.response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author: wizzardo
 * Date: 01.12.14
 */
public class CookieBuilder {

    protected static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format;
        }
    };

    private StringBuilder sb = new StringBuilder();

    public CookieBuilder(String key, String value) {
        sb.append(key).append('=').append(value);
    }

    public CookieBuilder expires(long date) {
        return expires(new Date(date));
    }

    public CookieBuilder expires(Date date) {
        sb.append(";Expires=").append(dateFormatThreadLocal.get().format(date));
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

    public String build() {
        return sb.toString();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
