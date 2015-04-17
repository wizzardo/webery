package com.wizzardo.http.mapping;

import java.util.HashMap;

/**
 * Created by wizzardo on 16.04.15.
 */
public class TemplatesHolder<K> {
    HashMap<K, UrlTemplate> templates = new HashMap<>();
    private String base;

    private TemplatesHolder(String host, int port) {
        this(host, port, null);
    }

    private TemplatesHolder(String host, int port, String context) {
        if (!host.startsWith("http")) {
            host = "http://" + host;
        }
        if (port != 80) {
            host += ":" + port;
        }
        if (context != null) {
            host += "/" + context;
        }
        base = host;
    }

    private TemplatesHolder(String base) {
        this.base = base;
    }

    public void add(K key, String url) {
        if (key == null)
            return;

        templates.put(key, new UrlTemplate(base, url));
    }

    public UrlTemplate getTemplate(K key) {
        return templates.get(key);
    }
}
