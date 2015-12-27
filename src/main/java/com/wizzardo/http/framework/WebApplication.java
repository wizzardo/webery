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
    protected Config config = new Config();

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
        ResourceTools localResources = environment == Environment.DEVELOPMENT ? new DevResourcesTools() : new LocalResourcesTools();
        File config = localResources.getResourceFile("Config.groovy");
        if (config != null && config.exists())
            EvalTools.prepare(FileTools.text(config)).get(this.config);

        if (this.config.containsKey("environments")) {
            Map environments = (Map) this.config.remove("environments");
            Map<String, Object> env = (Map<String, Object>) environments.get(environment.shortName);
            if (env != null)
                this.config.putAll(env);

            env = (Map<String, Object>) environments.get(environment.name().toLowerCase());
            if (env != null)
                this.config.putAll(env);
        }

        Holders.setApplication(this);

        List<Class> classes = localResources.getClasses();
        DependencyFactory.get().setClasses(classes);

        File staticResources = localResources.getResourceFile("public");
        if (staticResources != null && staticResources.exists())
            urlMapping.append("/static/*", "static", new FileTreeHandler<>(staticResources, "/static")
                    .setShowFolder(false));

        TagLib.findTags(classes);
        DependencyFactory.get().register(ResourceTools.class, new SingletonDependency<>(localResources));
        DependencyFactory.get().register(DecoratorLib.class, new SingletonDependency<>(new DecoratorLib(classes)));

        super.onStart();
        System.out.println("application has started");
        System.out.println("environment: " + environment);
    }

    protected void init() {
        super.init();
        DependencyFactory.get().register(UrlMapping.class, new SingletonDependency<>(urlMapping));

        MessageBundle bundle = initMessageSource();
        DependencyFactory.get().register(MessageSource.class, new SingletonDependency<>(bundle));
        DependencyFactory.get().register(MessageBundle.class, new SingletonDependency<>(bundle));
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
}
