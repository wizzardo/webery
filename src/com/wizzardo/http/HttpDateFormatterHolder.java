package com.wizzardo.http;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author: wizzardo
 * Date: 08.12.14
 */
public class HttpDateFormatterHolder {

    public static ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format;
        }
    };

    public static SimpleDateFormat get() {
        return formatter.get();
    }
}
