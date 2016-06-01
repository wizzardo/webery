package com.wizzardo.http.framework;

import com.wizzardo.epoll.IOThread;
import com.wizzardo.epoll.SslConfig;
import com.wizzardo.http.filter.AuthFilter;
import com.wizzardo.http.filter.BasicAuthFilter;
import com.wizzardo.http.filter.TokenFilter;
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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by wizzardo on 28.04.15.
 */
public class WebApplication extends HttpServer<HttpConnection> {

    protected Environment environment = Environment.DEVELOPMENT;
    protected Config config;
    protected ResourceTools resourcesTools;
    protected Consumer<WebApplication> onSetup;

    public WebApplication setEnvironment(Environment environment) {
        checkIfStarted();
        this.environment = environment;
        return this;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void onSetup(Consumer<WebApplication> onSetup) {
        this.onSetup = onSetup;
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

        Map environments = (Map) this.config.remove("environments");
        if (environments != null) {
            Map<String, Object> env = (Map<String, Object>) environments.get(environment.shortName);
            if (env != null)
                this.config.merge(env);

            env = (Map<String, Object>) environments.get(environment.name().toLowerCase());
            if (env != null)
                this.config.merge(env);
        }

        List<Class> classes = resourcesTools.getClasses();
        DependencyFactory.get().setClasses(classes);

        String staticResourcesPath = config.config("server").get("staticResourcesPath", "public");
        File staticResources = resourcesTools.getResourceFile(staticResourcesPath);
        if (staticResources != null && staticResources.exists())
            urlMapping.append("/static/*", new FileTreeHandler<>(staticResources, "/static", "static")
                    .setShowFolder(false));

        TagLib.findTags(classes);
        DependencyFactory.get().register(DecoratorLib.class, new SingletonDependency<>(new DecoratorLib(classes)));

        setupApplication();
        if (onSetup != null)
            onSetup.consume(this);

        super.onStart();
        System.out.println("application has started");
        System.out.println("environment: " + environment);
    }

    protected void setupApplication() {
        Config server = config.config("server");
        setHost(server.get("host", (String) null));
        setPort(server.get("port", 8080));
        setContext(server.get("context", (String) null));
        setDebugOutput(server.get("debugOutput", environment != Environment.PRODUCTION));

        int workers = server.get("ioWorkersCount", -1);
        if (workers > 0)
            setIoThreadsCount(workers);

        workers = server.get("workersCount", -1);
        if (workers > 0)
            setWorkersCount(workers);

        int ttl = server.get("ttl", -1);
        if (ttl > 0)
            setTTL(ttl);

        loadSslConfiguration(server);

        loadBasicAuthConfiguration(server);
    }

    protected void loadBasicAuthConfiguration(Config server) {
        Config basicAuth = server.config("basicAuth");
        String username = basicAuth.get("username", "");
        String password = basicAuth.get("password", "");
        if (!username.isEmpty() && !password.isEmpty()) {
            AuthFilter auth = new BasicAuthFilter();
            Boolean tokenEnabled = basicAuth.get("token", (Boolean) null);
            Config tokenized = basicAuth.config("tokenized");
            if ((tokenEnabled != null && tokenEnabled) || (tokenEnabled == null && !tokenized.isEmpty())) {
                TokenFilter tokenFilter = new TokenFilter(auth);
                long ttl = basicAuth.get("tokenTTL", -1l);
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

    protected void loadSslConfiguration(Config server) {
        Config ssl = server.config("ssl");
        String cert = ssl.get("cert", "");
        String key = ssl.get("key", "");
        if (!cert.isEmpty() && !key.isEmpty())
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
    }

    protected MessageBundle initMessageSource() {
        return new MessageBundle()
                .appendDefault("default.boolean.true", "true")
                .appendDefault("default.boolean.false", "false")
                ;
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
    public void setHost(String host) {
        super.setHost(host);
        config.config("server").put("host", host);
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
        config.config("server").config("session").put("ttl", sec);
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
