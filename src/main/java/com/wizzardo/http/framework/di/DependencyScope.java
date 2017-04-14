package com.wizzardo.http.framework.di;

import java.util.function.Supplier;

/**
 * Created by wizzardo on 27.04.15.
 */
public enum DependencyScope implements DependencyForge {
    SINGLETON(SingletonDependency::new),
    PROTOTYPE(PrototypeDependency::new),
    SESSION(SessionDependency::new),
    REQUEST(RequestDependency::new),
    THREAD_LOCAL(ThreadLocalDependency::new);

    private final SimpleForge defaultForge;

    DependencyScope(SimpleForge forge) {
        this.defaultForge = forge;
    }

    public <T> Dependency<? extends T> forge(Class<? extends T> clazz) {
        return forge(clazz, createSupplier(clazz), this);
    }

    public <T> Dependency<? extends T> forge(Class<? extends T> clazz, Supplier<T> supplier) {
        return forge(clazz, supplier, this);
    }

    @Override
    public <T> Dependency<? extends T> forge(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope) {
        return defaultForge.forge(clazz, supplier, scope);
    }

    private interface SimpleForge {
        <T> Dependency<? extends T> forge(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope);
    }
}
