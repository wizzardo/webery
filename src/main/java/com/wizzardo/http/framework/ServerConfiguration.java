package com.wizzardo.http.framework;

import com.wizzardo.tools.evaluation.Config;

/**
 * Created by wizzardo on 23/07/16.
 */
public class ServerConfiguration implements Configuration {
    public String hostname;
    public int port;
    public String context;
    public Session session;
    public boolean debugOutput;
    public int ioWorkersCount;
    public int workersCount;
    public long ttl;
    public SslConfig ssl;
    public BasicAuth basicAuth;

    public static class Session {
        public int ttl;
    }

    public static class SslConfig {
        public String key;
        public String cert;
    }

    public static class BasicAuth {
        public String username;
        public String password;
        public boolean token;
        public long tokenTTL;
        public Config tokenized;
    }

    public String prefix() {
        return "server";
    }
}
