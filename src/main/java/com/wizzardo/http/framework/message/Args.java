package com.wizzardo.http.framework.message;

import java.util.List;
import java.util.Objects;

/**
 * Created by wizzardo on 27.05.15.
 */
public interface Args {
    Object get(int i);

    static Args create(Object... args) {
        if (args == null)
            return i1 -> null;

        return i -> args.length <= i ? null : args[i];
    }

    static Args create(List args) {
        if (args == null)
            return i1 -> null;

        return i -> args.size() <= i ? null : args.get(i);
    }
}
