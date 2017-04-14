package com.wizzardo.http.framework.di;

import java.util.function.Supplier;

/**
 * Created by wizzardo on 05.05.15.
 */
public class ThreadLocalDependency<T> extends Dependency<T> {
    protected ThreadLocal<T> threadLocal = ThreadLocal.withInitial(this::newInstance);

    public ThreadLocalDependency(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope) {
        super(clazz, supplier, scope);
    }

    public ThreadLocalDependency(Class<? extends T> clazz) {
        super(clazz, DependencyScope.THREAD_LOCAL);
    }

    @Override
    public T get() {
        return threadLocal.get();
    }
}
