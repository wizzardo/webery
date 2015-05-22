package com.wizzardo.http.framework.message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 21.05.15.
 */
public abstract class MessageSource {
    public abstract String get(String key, Object... args);

    public String getWithDefault(String key, String def, Object... args) {
        String s = get(key, args);
        return s == null ? def : s;
    }

}
