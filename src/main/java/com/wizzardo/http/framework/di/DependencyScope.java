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

    private final DependencyForge forge;

    DependencyScope(DependencyForge forge) {
        this.forge = forge;
    }

    public <T> Dependency<? extends T> createDependency(Class<T> clazz) {
        return forge.forge(clazz);
    }
}
