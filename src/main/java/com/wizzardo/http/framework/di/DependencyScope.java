package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 27.04.15.
 */
public enum DependencyScope implements DependencyForge {
    SINGLETON(SingletonDependency::new),
    PROTOTYPE(PrototypeDependency::new),
    SESSION(SessionDependency::new),
    REQUEST(RequestDependency::new),
    THREAD_LOCAL(ThreadLocalDependency::new);

    private final DependencyForge defaultForge;

    DependencyScope(DependencyForge forge) {
        this.defaultForge = forge;
    }

    @Override
    public <T> Dependency<? extends T> forge(Class<T> clazz, DependencyScope scope) {
        return defaultForge.forge(clazz, scope);
    }
}
