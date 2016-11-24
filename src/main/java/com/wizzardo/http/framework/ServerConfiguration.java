package com.wizzardo.http.framework;

import com.wizzardo.tools.evaluation.Config;

/**
 * Created by wizzardo on 23/07/16.
 */
public class ServerConfiguration implements Configuration {
    public final String hostname;
    public final int port;
    public final String context;
    public final Session session;
    public final boolean debugOutput;
    public final int ioWorkersCount;
    public final int workersCount;
    public final long ttl;
    public final int postBodyLimit;
    public final int websocketFrameLengthLimit;
    public final int maxRequestsInQueue;
    public final SslConfig ssl;
    public final BasicAuth basicAuth;
    public final Resources resources;
    public final Multipart multipart;

    public ServerConfiguration(String hostname, int port, String context, Session session, boolean debugOutput, int ioWorkersCount, int workersCount, long ttl, int postBodyLimit, int websocketFrameLengthLimit, int maxRequestsInQueue, SslConfig ssl, BasicAuth basicAuth, Resources resources, Multipart multipart) {
        this.hostname = hostname;
        this.port = port;
        this.context = context;
        this.session = session;
        this.debugOutput = debugOutput;
        this.ioWorkersCount = ioWorkersCount;
        this.workersCount = workersCount;
        this.ttl = ttl;
        this.postBodyLimit = postBodyLimit;
        this.websocketFrameLengthLimit = websocketFrameLengthLimit;
        this.maxRequestsInQueue = maxRequestsInQueue;
        this.ssl = ssl;
        this.basicAuth = basicAuth;
        this.resources = resources;
        this.multipart = multipart;
    }

    public ServerConfiguration() {
        this(null, -1, null, null, false, -1, -1, -1, -1, -1, -1, null, null, null, null);
    }

    public static class Session {
        public final int ttl;

        public Session(int ttl) {
            this.ttl = ttl;
        }

        public Session() {
            this(-1);
        }
    }

    public static class Multipart {
        public final long limit;
        public final boolean enabled;

        public Multipart(int ttl, boolean enabled) {
            this.limit = ttl;
            this.enabled = enabled;
        }

        public Multipart() {
            this(-1, false);
        }
    }

    public static class SslConfig {
        public final String key;
        public final String cert;

        public SslConfig(String key, String cert) {
            this.key = key;
            this.cert = cert;
        }

        public SslConfig() {
            this(null, null);
        }
    }

    public static class BasicAuth {
        public final String username;
        public final String password;
        public final boolean token;
        public final long tokenTTL;
        public final Config tokenized;

        public BasicAuth(String username, String password, boolean token, long tokenTTL, Config tokenized) {
            this.username = username;
            this.password = password;
            this.token = token;
            this.tokenTTL = tokenTTL;
            this.tokenized = tokenized;
        }

        public BasicAuth() {
            this(null, null, false, -1, null);
        }
    }

    public static class Resources {
        public final String path;
        public final String mapping;
        public final Cache cache;

        public Resources(String path, String mapping, Cache cache) {
            this.path = path;
            this.mapping = mapping;
            this.cache = cache;
        }

        public Resources() {
            this(null, null, null);
        }

        public static class Cache {
            public final long ttl;
            public final long memoryLimit;
            public final long maxFileSize;
            public final boolean enabled;
            public final boolean gzip;

            public Cache(long ttl, long memoryLimit, long maxFileSize, boolean enabled, boolean gzip) {
                this.ttl = ttl;
                this.memoryLimit = memoryLimit;
                this.maxFileSize = maxFileSize;
                this.enabled = enabled;
                this.gzip = gzip;
            }

            public Cache() {
                this(-1, -1, -1, false, false);
            }
        }
    }

    public String prefix() {
        return "server";
    }
}
