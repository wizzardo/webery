package com.wizzardo.http.mapping;

import java.util.HashMap;

/**
 * Created by wizzardo on 16.04.15.
 */
public class TemplatesHolder<K> {
    protected HashMap<K, UrlTemplate> templates = new HashMap<>();
    protected String base;
    protected String context;
    protected String host;
    protected boolean https;
    protected int port;

    public TemplatesHolder() {
        port = 8080;
        updateBase();
    }

    public TemplatesHolder(String host, int port) {
        this(host, port, null);
    }

    public TemplatesHolder(String host, int port, String context) {
        this(host, port, false, context);
    }

    public TemplatesHolder(String host, int port, boolean https, String context) {
        this.host = host;
        this.port = port;
        this.https = https;
        this.context = context;
        updateBase();
    }

    protected void updateBase() {
        String host = this.host;
        if (host == null)
            host = "localhost";

        if (!host.startsWith("http"))
            host = (https ? "https://" : "http://") + host;

        if (port != 80 && port != 443)
            host += ":" + port;

        base = host;
    }

    public TemplatesHolder<K> setHost(String host) {
        this.host = "0.0.0.0".equals(host) ? "localhost" : host;
        updateBase();
        return this;
    }

    public TemplatesHolder<K> setPort(int port) {
        this.port = port;
        updateBase();
        return this;
    }

    public TemplatesHolder<K> setIsHttps(boolean https) {
        this.https = https;
        updateBase();
        return this;
    }

    public TemplatesHolder<K> setContext(String context) {
        this.context = context;
        return this;
    }

    public TemplatesHolder<K> append(K key, String url) {
        if (key == null)
            return this;

        templates.put(key, new UrlTemplate(this, url));
        return this;
    }

    public UrlTemplate getTemplate(K key) {
        return templates.get(key);
    }
}
