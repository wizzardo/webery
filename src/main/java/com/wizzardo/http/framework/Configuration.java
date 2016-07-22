package com.wizzardo.http.framework;

import com.wizzardo.http.framework.di.PostConstruct;
import com.wizzardo.tools.evaluation.Config;

/**
 * Created by wizzardo on 22/07/16.
 */
public interface Configuration extends PostConstruct {

    default void init() {
        getConfig().bind(this);
    }

    default Config getConfig() {
        String[] path = prefix().split("\\.");

        Config config = Holders.getConfig();
        if (path.length == 0) {
            return config;
        } else {
            for (String entry : path) {
                config = config.config(entry);
            }
            return config;
        }
    }

    default String prefix() {
        return "";
    }
}
