package com.wizzardo.http.mapping;

import java.util.HashMap;

/**
 * Created by wizzardo on 16.04.15.
 */
public class TemplatesHolder<K> {
    protected HashMap<K, UrlTemplate> templates = new HashMap<>();
    protected String base;
    protected String context;

    public TemplatesHolder(String host, int port) {
        this(host, port, null);
    }

    public TemplatesHolder(String host, int port, String context) {
        if (!host.startsWith("http"))
            host = "http://" + host;

        if (port != 80)
            host += ":" + port;


        this.context = context;
        base = host;
    }

    public TemplatesHolder(String base) {
        this.base = base;
    }

    public TemplatesHolder<K> append(K key, String url) {
        if (key == null)
            return this;

        templates.put(key, new UrlTemplate(base, context, url));
        return this;
    }

    public UrlTemplate getTemplate(K key) {
        return templates.get(key);
    }
}
