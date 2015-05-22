package com.wizzardo.http.framework.message;

/**
 * Created by wizzardo on 21.05.15.
 */
public interface MessageSource {
    String get(String key, Object... args);

    default String getWithDefault(String key, String def, Object... args) {
        String s = get(key, args);
        return s == null ? def : s;
    }
}
