package com.wizzardo.http.framework;

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

    public WebApplication() {
    }

    public WebApplication(int port) {
    }

    public WebApplication(String host, int port) {
        this(host, port, null, 4);
    }

    public WebApplication(String host, int port, String context) {
        this(host, port, context, 4);
    }

    public WebApplication(String host, int port, int workersCount) {
        this(host, port, null, workersCount);
    }

    public WebApplication(String host, int port, String context, int workersCount) {
        super(host, port, context, workersCount);
    }

    public WebApplication setEnvironment(Environment environment) {
        checkIfStarted();
        this.environment = environment;
        return this;
    }

    public Environment getEnvironment() {
        return environment;
    }

    protected void onStart() {
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

        File staticResources = resourcesTools.getResourceFile("public");
        if (staticResources != null && staticResources.exists())
            urlMapping.append("/static/*", "static", new FileTreeHandler<>(staticResources, "/static")
                    .setShowFolder(false));

        TagLib.findTags(classes);
        DependencyFactory.get().register(DecoratorLib.class, new SingletonDependency<>(new DecoratorLib(classes)));

        super.onStart();
        System.out.println("application has started");
        System.out.println("environment: " + environment);
    }

    protected ResourceTools createResourceTools() {
        File src = new File("src");
        return src.exists() && src.isDirectory() ? new DevResourcesTools() : new LocalResourcesTools();
    }

    protected void init() {
        super.init();
        Holders.setApplication(this);
        resourcesTools = createResourceTools();
        DependencyFactory.get().register(ResourceTools.class, new SingletonDependency<>(resourcesTools));
        DependencyFactory.get().register(UrlMapping.class, new SingletonDependency<>(urlMapping));

        MessageBundle bundle = initMessageSource();
        DependencyFactory.get().register(MessageSource.class, new SingletonDependency<>(bundle));
        DependencyFactory.get().register(MessageBundle.class, new SingletonDependency<>(bundle));

        config = new Config();
        loadConfig("Config.groovy");
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
}
