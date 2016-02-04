package com.wizzardo.http;

/**
 * Created by wizzardo on 05.02.16.
 */
public interface Named {
    default String name() {
        return null;
    }
}
