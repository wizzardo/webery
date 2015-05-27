package com.wizzardo.http.framework.message;

import java.util.List;

/**
 * Created by wizzardo on 21.05.15.
 */
public interface MessageSource {

    String get(String key, Args args);

    default String get(String key, Object... args) {
        return get(key, Args.create(args));
    }

    default String get(String key, List args) {
        return get(key, Args.create(args));
    }

    default String getWithDefault(String key, String def, Object... args) {
        String s = get(key, args);
        return s == null ? def : s;
    }
}
