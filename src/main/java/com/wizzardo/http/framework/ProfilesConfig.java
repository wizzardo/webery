package com.wizzardo.http.framework;

import com.wizzardo.tools.evaluation.Config;

/**
 * Created by wizzardo on 16/10/16.
 */
public class ProfilesConfig extends Config {
    public ProfilesConfig() {
    }

    public ProfilesConfig(Config parent, String name) {
        super(parent, name);
    }

    @Override
    public Config root() {
        if (parent == null)
            return this;

        if ("profiles".equals(parent.name()))
            return this;

        if ("environments".equals(parent.name()))
            return this;

        return parent.root();
    }

    @Override
    public Object put(String key, Object value) {
        return super.put(key, value);
    }

    @Override
    protected Config createConfig(Config parent, String name) {
        return new ProfilesConfig(parent, name);
    }

    @Override
    protected Config createProxyConfig(Config main, String name, Config parent, Config proxy) {
        return new ProxyProfilesConfig(main, name, parent, proxy);
    }

    static class ProxyProfilesConfig extends Config.ProxyConfig {
        public ProxyProfilesConfig(Config main, String key, Config parent, Config proxy) {
            super(main, key, parent, proxy);
        }

        @Override
        protected Config createConfig(Config parent, String name) {
            return new ProfilesConfig(parent, name);
        }

        @Override
        protected Config createProxyConfig(Config main, String name, Config parent, Config proxy) {
            return new ProxyProfilesConfig(main, name, parent, proxy);
        }
    }
}
