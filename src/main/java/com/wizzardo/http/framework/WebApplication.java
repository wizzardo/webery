package com.wizzardo.http.framework;

import com.wizzardo.http.*;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.SingletonDependency;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.message.MessageSource;
import com.wizzardo.http.framework.template.DecoratorLib;
import com.wizzardo.http.framework.template.LocalResourcesTools;
import com.wizzardo.http.framework.template.ResourceTools;
import com.wizzardo.http.framework.template.TagLib;
import com.wizzardo.http.mapping.UrlMapping;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by wizzardo on 28.04.15.
 */
public class WebApplication extends HttpServer<HttpConnection> {

    protected Environment environment = Environment.DEVELOPMENT;
    private volatile boolean started = false;

    public WebApplication(int port) {
        this(null, port);
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
        if (started)
            throw new IllegalStateException("Application already started, cannot set environment");

        this.environment = environment;
        return this;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void run() {
        started = true;
        super.run();
    }

    protected void init() {
        super.init();
        ResourceTools localResources = new LocalResourcesTools();
        List<Class> classes = localResources.getClasses();
        DependencyFactory.get().setClasses(classes);
        urlMapping.append("/static/*", "static", new FileTreeHandler<>(localResources.getResourceFile("public"), "/static")
                .setShowFolder(false));

        TagLib.findTags(classes);
        DependencyFactory.get().register(UrlMapping.class, new SingletonDependency<>(urlMapping));
        DependencyFactory.get().register(ResourceTools.class, new SingletonDependency<>(localResources));

        MessageBundle bundle = initMessageSource();
        DependencyFactory.get().register(MessageSource.class, new SingletonDependency<>(bundle));
        DependencyFactory.get().register(MessageBundle.class, new SingletonDependency<>(bundle));
        DependencyFactory.get().register(DecoratorLib.class, new SingletonDependency<>(new DecoratorLib(classes)));
    }

    protected MessageBundle initMessageSource() {
        return new MessageBundle()
                .appendDefault("default.boolean.true", "true")
                .appendDefault("default.boolean.false", "false")
                ;
    }

    @Override
    protected Worker<HttpConnection> createWorker(BlockingQueue<HttpConnection> queue, String name) {
        return new WebWorker<HttpConnection>(queue, name) {
            @Override
            protected void process(HttpConnection connection) {
                processConnection(connection);
            }
        };
    }

    @Override
    public ControllerUrlMapping getUrlMapping() {
        return (ControllerUrlMapping) super.getUrlMapping();
    }

    @Override
    protected ControllerUrlMapping createUrlMapping(String host, int port, String context) {
        return new ControllerUrlMapping(host, port, context);
    }
}
