package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 27.04.15.
 */
public enum DependencyScope {
    SINGLETON,
    PROTOTYPE,
    SESSION,
    REQUEST,
    THREAD_LOCAL;

    public <T> Dependency<T> createDependency(Class<T> clazz) {
        switch (this) {
            case SINGLETON:
                return new SingletonDependency<>(clazz);
            case PROTOTYPE:
                return new PrototypeDependency<>(clazz);
            case SESSION:
                return new SessionDependency<>(clazz);
            case THREAD_LOCAL:
                return new ThreadLocalDependency<>(clazz);
            case REQUEST:
                return new RequestDependency<>(clazz);
            default:
                throw new IllegalStateException("Unknown scope: " + this);
        }
    }
}
