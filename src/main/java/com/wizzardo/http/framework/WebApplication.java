package com.wizzardo.http.framework;

import com.wizzardo.epoll.IOThread;
import com.wizzardo.epoll.SslConfig;
import com.wizzardo.http.*;
import com.wizzardo.http.filter.AuthFilter;
import com.wizzardo.http.filter.BasicAuthFilter;
import com.wizzardo.http.filter.TokenFilter;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.SingletonDependency;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.message.MessageSource;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.decorator.LayoutBody;
import com.wizzardo.http.framework.template.decorator.LayoutHead;
import com.wizzardo.http.framework.template.decorator.LayoutTitle;
import com.wizzardo.http.framework.template.taglib.g.*;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.RangeResponseHelper;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.collections.flow.Flow;
import com.wizzardo.tools.evaluation.Config;
import com.wizzardo.tools.evaluation.EvalTools;
import com.wizzardo.tools.interfaces.Consumer;
import com.wizzardo.tools.misc.TextTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.jar.Manifest;

/**
 * Created by wizzardo on 28.04.15.
 */
public class WebApplication extends HttpServer<HttpConnection> {

    protected Environment environment;
    protected Config config;
    protected Map<String, String> cliArgs = Collections.emptyMap();
    protected ResourceTools resourcesTools;
    protected Consumer<WebApplication> onSetup;
    protected Consumer<WebApplication> onLoadConfiguration;
    protected Set<String> profiles;

    public WebApplication() {
    }

    public WebApplication(String[] args) {
        Map<String, String> map = parseCliArgs(args);
        processArgs(map::get);
        cliArgs = map;
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
        if (this.onSetup == null)
            this.onSetup = onSetup;
        else {
            Consumer<WebApplication> setup = this.onSetup;
            this.onSetup = it -> {
                setup.consume(it);
                onSetup.consume(it);
            };
        }
        return this;
    }

    public WebApplication onLoadConfiguration(Consumer<WebApplication> onLoadConfiguration) {
        this.onLoadConfiguration = onLoadConfiguration;
        return this;
    }

    protected List<Class<? extends Tag>> getBasicTags() {
        ArrayList<Class<? extends Tag>> list = new ArrayList<>();
        list.add(CheckBox.class);
        list.add(Collect.class);
        list.add(CreateLink.class);
        list.add(Each.class);
        list.add(Else.class);
        list.add(Elseif.class);
        list.add(Form.class);
        list.add(FormatBoolean.class);
        list.add(HiddenField.class);
        list.add(If.class);
        list.add(Join.class);
        list.add(Link.class);
        list.add(Message.class);
        list.add(PasswordField.class);
        list.add(Radio.class);
        list.add(Resource.class);
        list.add(com.wizzardo.http.framework.template.taglib.g.Set.class);
        list.add(TextArea.class);
        list.add(TextField.class);
        list.add(While.class);
        return list;
    }

    protected List<Class<? extends Decorator>> getBasicDecorators() {
        ArrayList<Class<? extends Decorator>> list = new ArrayList<>();
        list.add(LayoutBody.class);
        list.add(LayoutHead.class);
        list.add(LayoutTitle.class);
        return list;
    }

    protected void onStart() {
        resourcesTools = createResourceTools();
        List<Class> classes = resourcesTools.getClasses();
        if (!classes.contains(getBasicDecorators().get(0))) {
            classes.addAll(getBasicTags());
            classes.addAll(getBasicDecorators());
        }

        DependencyFactory.get().setClasses(classes);

        DependencyFactory.get().register(ResourceTools.class, new SingletonDependency<>(resourcesTools));
        DependencyFactory.get().register(UrlMapping.class, new SingletonDependency<>(getUrlMapping()));
        DependencyFactory.get().register(ControllerUrlMapping.class, new SingletonDependency<>(getUrlMapping()));

        MessageBundle bundle = initMessageSource();
        DependencyFactory.get().register(MessageSource.class, new SingletonDependency<>(bundle));
        DependencyFactory.get().register(MessageBundle.class, new SingletonDependency<>(bundle));

        loadConfig("Config.groovy");
        processListener(onLoadConfiguration);
        readProfiles(config);

        loadEnvironmentVariables(config);
        loadSystemProperties(config);
        cliArgs.forEach((key, value) -> putInto(config, key, value));

        setupApplication();
        processListener(onSetup);

        TagLib.findTags(classes);
        DependencyFactory.get().register(DecoratorLib.class, new SingletonDependency<>(new DecoratorLib(classes)));

        super.onStart();
        System.out.println("application has started on port " + server.getPort());
        System.out.println("environment: " + environment);
        System.out.println("profiles: " + profiles);
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
            config.merge(subConfig);
        }

        if (this.config != config)
            this.config.merge(config);
    }

    protected void setupApplication() {
        ServerConfiguration server = DependencyFactory.get(ServerConfiguration.class);

        super.setHostname(server.hostname);
        super.setPort(server.port);
        super.setDebugOutput(server.debugOutput);
        super.setPostBodyLimit(server.postBodyLimit);
        super.setWebsocketFrameLengthLimit(server.websocketFrameLengthLimit);
        super.setOnlyCachedHeaders(server.onlyCachedHeaders);
        super.setMaxRequestsInQueue(server.maxRequestsInQueue);
        super.setSessionTimeout(server.session.ttl);
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

        createStaticResourcesHandler(resources);
    }

    protected void createStaticResourcesHandler(ServerConfiguration.Resources resources) {
        String resourcesPath = resources.path;
        File staticResources = resourcesTools.getResourceFile(resourcesPath);
        if (staticResources != null && staticResources.exists()) {
            FileTreeHandler<FileTreeHandler.HandlerContext> handler = new FileTreeHandler<>(staticResources, resources.mapping, "resources")
                    .setShowFolder(false)
                    .setRangeResponseHelper(new RangeResponseHelper(resources.cache));

            DependencyFactory.get().register(FileTreeHandler.class, new SingletonDependency<>(handler));
            urlMapping.append(resources.mapping + "/*", handler);
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
        ResourceTools resourceTools;
        if (environment == Environment.TEST) {
            resourceTools = new TestResourcesTools();
        } else {
            File src = new File("src");
            resourceTools = src.exists() && src.isDirectory() ? new DevResourcesTools() : new LocalResourcesTools();
        }

        return resourceTools
                .addClasspathFilter(name -> name.startsWith("com.wizzardo.http.framework"));
    }

    protected void init() {
        super.init();
        profiles = new LinkedHashSet<>();
        environment = Environment.DEVELOPMENT;
        Holders.setApplication(this);
        config = new ProfilesConfig();
        loadDefaultConfiguration(config);
        loadManifest(config);
        processArgs(System::getProperty);
    }

    protected void processArgs(CollectionTools.Closure<String, String> args) {
        String value;
        if ((value = args.execute("env")) != null)
            environment = Environment.parse(value);

        if ((value = args.execute("environment")) != null)
            environment = Environment.parse(value);

        if ((value = args.execute("profiles.active")) != null)
            Flow.of(value.split(",")).each(this::addProfile).execute();
    }

    protected void loadDefaultConfiguration(Config config) {
        Config server = config.config("server");

        server.put("hostname", "0.0.0.0");
        server.put("port", 8080);
        server.put("debugOutput", false);
        server.put("postBodyLimit", 2 * 1024 * 1024);
        server.put("websocketFrameLengthLimit", 64 * 1024);
        server.put("maxRequestsInQueue", 1000);

        Config session = server.config("session");
        session.put("ttl", 30 * 60);

        Config resources = server.config("resources");
        resources.put("path", "public");
        resources.put("mapping", "/static");

        Config resourcesCache = resources.config("cache");
        resourcesCache.put("ttl", -1L);
        resourcesCache.put("memoryLimit", 32 * 1024 * 1024L);
        resourcesCache.put("maxFileSize", 5 * 1024 * 1024L);
        resourcesCache.put("enabled", true);
        resourcesCache.put("gzip", true);

        Config multipart = server.config("multipart");
        multipart.put("limit", -1L);
        multipart.put("enabled", false);

        Config renderer = server.config("renderer");
        renderer.put("viewCacheTtl", -1L);
        renderer.put("templateCacheTtl", -1L);

        loadDefaultDevelopmentConfiguration(config.config("environments").config("dev"));
    }

    protected void loadDefaultDevelopmentConfiguration(Config config) {
        Config server = config.config("server");
        server.put("debugOutput", true);

        Config resources = server.config("resources");
        Config resourcesCache = resources.config("cache");
        resourcesCache.put("enabled", false);

        Config renderer = server.config("renderer");
        renderer.put("viewCacheTtl", 1L);
        renderer.put("templateCacheTtl", 1L);
    }

    protected void loadEnvironmentVariables(Config config) {
        System.getenv().forEach(config::put);
    }

    protected void loadSystemProperties(Config config) {
        System.getProperties().forEach((key, value) -> putInto(config, String.valueOf(key), value));
    }

    protected void putInto(Config config, String key, Object value) {
        String[] keys = key.split("\\.");
        Config subConfig = config;
        int last = keys.length - 1;
        int i = 0;
        try {
            for (; i < last; i++) {
                subConfig = subConfig.config(keys[i]);
            }
            subConfig.put(keys[last], value);
        } catch (ClassCastException e) {
            String k = "";
            for (int j = 0; j < i; j++) {
                k += keys[j] + ".";
            }
            k += keys[i];
            System.out.println("WARNING! cannot overwrite config value " + k + "=" + subConfig.get(keys[i]) + " with new config " + key + "=" + value);
        }
    }

    protected void loadManifest(Config config) {
        Unchecked.ignore(() -> {
            Manifest manifest = new Manifest(WebApplication.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
            Config subconfig = config.config("manifest");
            manifest.getMainAttributes().forEach((k, v) -> putInto(subconfig, String.valueOf(k), String.valueOf(v)));
        });
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
    protected boolean processConnection(HttpConnection connection) {
        RequestContext context = (RequestContext) Thread.currentThread();
        context.getRequestHolder().set(connection.getRequest(), connection.getResponse());
        try {
            return super.processConnection(connection);
        } finally {
            context.reset();
        }
    }

    @Override
    protected Response handle(Request request, Response response, Handler handler) throws IOException {
        ((RequestContext) Thread.currentThread()).handler(handler.name());
        return super.handle(request, response, handler);
    }

    @Override
    protected Worker<HttpConnection> createWorker(ThreadGroup group, BlockingQueue<HttpConnection> queue, String name) {
        return new WebWorker<>(this, group, queue, name);
    }

    @Override
    protected IOThread<? extends HttpConnection> createIOThread(int number, int divider) {
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
        String text = resourcesTools.getResourceAsString(path);
        if (text != null) {
            System.out.println("load config from: " + path);
            EvalTools.prepare(text).get(config);
        }
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
