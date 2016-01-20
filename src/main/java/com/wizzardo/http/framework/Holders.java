package com.wizzardo.http.framework;

import com.wizzardo.tools.evaluation.Config;

/**
 * Created by wizzardo on 22.12.15.
 */
public class Holders {

    private static WebApplication application;

    public static WebApplication getApplication() {
        return application;
    }

    public static Environment getEnvironment() {
        return application != null ? application.getEnvironment() : null;
    }

    public static Config getConfig() {
        return application != null ? application.getConfig() : null;
    }

    static void setApplication(WebApplication app) {
        application = app;
    }
}


