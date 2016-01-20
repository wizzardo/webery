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

    public static Environment parse(String s) {
        if (DEVELOPMENT.shortName.equals(s))
            return DEVELOPMENT;
        if (PRODUCTION.shortName.equals(s))
            return PRODUCTION;
        if (TEST.shortName.equals(s))
            return TEST;

        return Environment.valueOf(s == null ? null : s.toUpperCase());
    }
}
