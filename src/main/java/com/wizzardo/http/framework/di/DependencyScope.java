package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 27.04.15.
 */
public enum DependencyScope {
    SINGLETON(SingletonDependency::new),
    PROTOTYPE(PrototypeDependency::new),
    SESSION(SessionDependency::new),
    REQUEST(RequestDependency::new),
    THREAD_LOCAL(ThreadLocalDependency::new);

    private final DependencyFactory factory;

    DependencyScope(DependencyFactory factory) {
        this.factory = factory;
    }

    public <T> Dependency<T> createDependency(Class<T> clazz) {
        return factory.create(clazz);
    }

    private interface DependencyFactory {
        <T> Dependency<T> create(Class<T> clazz);
    }
}
