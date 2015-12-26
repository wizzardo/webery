package com.wizzardo.http.framework;

/**
 * Created by wizzardo on 08.07.15.
 */
public enum Environment {
    DEVELOPMENT("dev"), TEST("test"), PRODUCTION("prod");

    public final String shortName;

    Environment(String shortName) {
        this.shortName = shortName;
    }
}
