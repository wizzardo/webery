package com.wizzardo.http.framework;

import com.wizzardo.epoll.IOThread;
import com.wizzardo.epoll.SslConfig;
import com.wizzardo.http.filter.AuthFilter;
import com.wizzardo.http.filter.BasicAuthFilter;
import com.wizzardo.http.filter.TokenFilter;
import com.wizzardo.http.response.RangeResponseHelper;
import com.wizzardo.tools.collections.flow.Flow;
import com.wizzardo.tools.evaluation.Config;
import com.wizzardo.http.*;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.SingletonDependency;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.message.MessageSource;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.tools.evaluation.EvalTools;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.Consumer;
import com.wizzardo.tools.misc.TextTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Created by wizzardo on 28.04.15.
 */
public class WebApplication extends HttpServer<HttpConnection> {

    protected Environment environment = Environment.DEVELOPMENT;
    protected Config config;
    protected ResourceTools resourcesTools;
    protected Consumer<WebApplication> onSetup;
    protected Consumer<WebApplication> onLoadConfiguration;
    protected Map<String, String> args;
    protected Set<String> profiles = new LinkedHashSet<>();

    public WebApplication() {
    }

    public WebApplication(String[] args) {
        this.args = parseCliArgs(args);
    }

    public WebApplication setEnvironment(Environment environment) {
        checkIfStarted();
        this.environment = environment;
        return this;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Set<String> getProfiles() {
        return profiles;
    }

    public WebApplication addProfile(String profile) {
        checkIfStarted();
        profiles.add(profile);
        return this;
    }

    public WebApplication onSetup(Consumer<WebApplication> onSetup) {
        this.onSetup = onSetup;
        return this;
    }

    public WebApplication onLoadConfiguration(Consumer<WebApplication> onLoadConfiguration) {
        this.onLoadConfiguration = onLoadConfiguration;
        return this;
    }

    protected void onStart() {
        resourcesTools = createResourceTools();
        DependencyFactory.get().register(ResourceTools.class, new SingletonDependency<>(resourcesTools));
        DependencyFactory.get().register(UrlMapping.class, new SingletonDependency<>(getUrlMapping()));
        DependencyFactory.get().register(ControllerUrlMapping.class, new SingletonDependency<>(getUrlMapping()));

        MessageBundle bundle = initMessageSource();
        DependencyFactory.get().register(MessageSource.class, new SingletonDependency<>(bundle));
        DependencyFactory.get().register(MessageBundle.class, new SingletonDependency<>(bundle));

        loadConfig("Config.groovy");
        processListener(onLoadConfiguration);

        readProfiles(config);

        List<Class> classes = resourcesTools.getClasses();
        DependencyFactory.get().setClasses(classes);

        TagLib.findTags(classes);
        DependencyFactory.get().register(DecoratorLib.class, new SingletonDependency<>(new DecoratorLib(classes)));

        setupApplication();
        processListener(onSetup);

        super.onStart();
        System.out.println("application has started");
        System.out.println("environment: " + environment);
    }

    protected void readProfiles(Config config) {
        if (config == null || config.isEmpty())
            return;

        Config environments = (Config) config.remove("environments");
        if (environments != null) {
            Config env = environments.config(environment.shortName);
            if (env != null)
                config.merge(env);

            env = environments.config(environment.name().toLowerCase());
            if (env != null)
                config.merge(env);
        }

        Config profiles = (Config) config.remove("profiles");
        if (profiles == null || profiles.isEmpty())
            return;

        for (String profile : this.profiles) {
            Config subConfig = profiles.config(profile);
            readProfiles(subConfig);
            this.config.merge(subConfig);
        }

        if (this.config != config)
            this.config.merge(config);
    }

    protected void setupApplication() {
        ServerConfiguration server = DependencyFactory.get(ServerConfiguration.class);

        super.setHostname(server.hostname);
        super.setPort(server.port);
        super.setDebugOutput(server.debugOutput);
        super.setSessionTimeout(server.session.ttlSeconds);
        setContext(server.context);

        int workers = server.ioWorkersCount;
        if (workers > 0)
            super.setIoThreadsCount(workers);

        workers = server.workersCount;
        if (workers > 0)
            super.setWorkersCount(workers);

        long ttl = server.ttl;
        if (ttl > 0)
            super.setTTL(ttl);

        loadSslConfiguration(server.ssl);
        loadBasicAuthConfiguration(server.basicAuth);
        loadResourcesConfiguration(server.resources);
    }

    protected void loadResourcesConfiguration(ServerConfiguration.Resources resources) {
        if (resources == null)
            return;

        if (TextTools.isBlank(resources.mapping))
            throw new IllegalArgumentException("server.resources.mapping cannot be null or empty");

        String resourcesPath = resources.path;
        File staticResources = resourcesTools.getResourceFile(resourcesPath);
        if (staticResources != null && staticResources.exists()) {
            FileTreeHandler<FileTreeHandler.HandlerContext> handler = new FileTreeHandler<>(staticResources, "/" + resources.mapping, "resources")
                    .setShowFolder(false)
                    .setRangeResponseHelper(new RangeResponseHelper(resources.cache));

            DependencyFactory.get().register(FileTreeHandler.class, new SingletonDependency<>(handler));
            urlMapping.append("/" + resources.mapping + "/*", handler);
        }
    }

    protected void loadBasicAuthConfiguration(ServerConfiguration.BasicAuth basicAuth) {
        if (basicAuth == null)
            return;

        String username = basicAuth.username;
        String password = basicAuth.password;
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            AuthFilter auth = new BasicAuthFilter();
            boolean tokenEnabled = basicAuth.token;
            Config tokenized = basicAuth.tokenized;
            if (tokenEnabled) {
                TokenFilter tokenFilter = new TokenFilter(auth);
                long ttl = basicAuth.tokenTTL;
                if (ttl > 0)
                    tokenFilter.setTTL(ttl);

                DependencyFactory.get().register(TokenFilter.class, new SingletonDependency<>(tokenFilter));
                auth = tokenFilter;
                for (String name : tokenized.keySet()) {
                    urlMapping.append("/" + name + "/*", new FileTreeHandler<>(tokenized.get(name, ""), "/" + name, name)
                            .setShowFolder(false));
                }
            }
            auth.allow(username, password);
            filtersMapping.addBefore("/*", auth);
        }
    }

    protected void loadSslConfiguration(ServerConfiguration.SslConfig ssl) {
        if (ssl == null)
            return;

        String cert = ssl.cert;
        String key = ssl.key;
        if (cert != null && !cert.isEmpty() && key != null && !key.isEmpty())
            loadCertificates(cert, key);
    }

    protected ResourceTools createResourceTools() {
        if (environment == Environment.TEST)
            return new TestResourcesTools();

        File src = new File("src");
        return src.exists() && src.isDirectory() ? new DevResourcesTools() : new LocalResourcesTools();
    }

    protected void init() {
        super.init();
        Holders.setApplication(this);
        config = new Config();
        loadDefaultConfiguration(config);
        loadEnvironmentVariables(config);
        loadSystemProperties(config);
        processCliArgs();
    }

    protected void processCliArgs() {
        if (args == null || args.isEmpty())
            return;

        String value;
        if ((value = args.get("env")) != null)
            environment = Environment.parse(value);

        if ((value = args.get("environment")) != null)
            environment = Environment.parse(value);

        if ((value = args.get("profiles")) != null)
            Flow.of(value.split(",")).each(this::addProfile).execute();
    }

    protected void loadDefaultConfiguration(Config config) {
        Config server = config.config("server");

        server.put("hostname", "0.0.0.0");
        server.put("port", 8080);
        server.put("debugOutput", false);

        Config session = server.config("session");
        session.put("ttlSeconds", 30 * 60);

        Config resources = server.config("resources");
        resources.put("path", "public");
        resources.put("mapping", "static");

        Config resourcesCache = resources.config("cache");
        resourcesCache.put("ttl", -1L);
        resourcesCache.put("memoryLimit", 32 * 1024 * 1024L);
        resourcesCache.put("maxFileSize", 5 * 1024 * 1024L);
        resourcesCache.put("enabled", true);
        resourcesCache.put("gzip", true);
    }

    protected void loadEnvironmentVariables(Config config) {
        Unchecked.ignore(() -> System.getenv().forEach(config::put));
    }

    protected void loadSystemProperties(Config config) {
        Unchecked.ignore(() -> System.getProperties().forEach((key, value) -> {
            String[] keys = String.valueOf(key).split("\\.");
            Config subConfig = config;
            int last = keys.length - 1;
            for (int i = 0; i < last; i++) {
                subConfig = subConfig.config(keys[i]);
            }
            subConfig.put(keys[last], value);
        }));
    }

    protected Map<String, String> parseCliArgs(String[] args) {
        Map<String, String> map = new HashMap<>(args.length, 1);
        for (String arg : args) {
            String[] kv = arg.split("=", 2);
            map.put(kv[0], kv.length == 2 ? kv[1] : null);
        }
        return map;
    }

    protected MessageBundle initMessageSource() {
        return new MessageBundle()
                .appendDefault("default.boolean.true", "true")
                .appendDefault("default.boolean.false", "false")
                ;
    }

    protected void processListener(Consumer<WebApplication> listner) {
        if (listner != null)
            listner.consume(this);
    }

    @Override
    protected Worker<HttpConnection> createWorker(BlockingQueue<HttpConnection> queue, String name) {
        return new WebWorker<>(this, queue, name);
    }

    @Override
    protected IOThread<HttpConnection> createIOThread(int number, int divider) {
        return new WebIOThread<>(this, number, divider);
    }

    @Override
    public ControllerUrlMapping getUrlMapping() {
        return (ControllerUrlMapping) super.getUrlMapping();
    }

    @Override
    protected ControllerUrlMapping createUrlMapping() {
        return new ControllerUrlMapping();
    }

    public Config getConfig() {
        return config;
    }

    public WebApplication loadConfig(String path) {
        resourcesTools.getResourceFile(path, file -> {
            System.out.println("load config from: " + file.getAbsolutePath());
            EvalTools.prepare(FileTools.text(file)).get(config);
        });
        return this;
    }

    @Override
    public void setHostname(String hostname) {
        super.setHostname(hostname);
        config.config("server").put("hostname", hostname);
    }

    @Override
    public void setPort(int port) {
        super.setPort(port);
        config.config("server").put("port", port);
    }

    public WebApplication setDebugOutput(boolean enabled) {
        super.setDebugOutput(enabled);
        config.config("server").put("debugOutput", enabled);
        return this;
    }

    @Override
    public void setSessionTimeout(int sec) {
        super.setSessionTimeout(sec);
        config.config("server").config("session").put("ttlSeconds", sec);
    }

    @Override
    public void setIoThreadsCount(int ioThreadsCount) {
        super.setIoThreadsCount(ioThreadsCount);
        config.config("server").put("ioWorkersCount", ioThreadsCount);
    }

    @Override
    public void setWorkersCount(int count) {
        super.setWorkersCount(count);
        config.config("server").put("workersCount", count);
    }

    @Override
    public void setContext(String context) {
        while (context != null && context.startsWith("/"))
            context = context.substring(1);

        if (context != null && context.isEmpty())
            context = null;

        super.setContext(context);
        config.config("server").put("context", context);
    }

    @Override
    public void setTTL(long milliseconds) {
        super.setTTL(milliseconds);
        config.config("server").put("ttl", milliseconds);
    }

    @Override
    public void loadCertificates(SslConfig sslConfig) {
        super.loadCertificates(sslConfig);
        config.config("server").config("ssl").put("cert", sslConfig.getCertFile());
        config.config("server").config("ssl").put("key", sslConfig.getKeyFile());
    }
}
