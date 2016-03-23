package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 05.05.15.
 */
public class ThreadLocalDependency<T> extends Dependency<T> {
    protected ThreadLocal<T> threadLocal = new ThreadLocal<T>() {
        @Override
        protected T initialValue() {
            return newInstance();
        }
    };

    public ThreadLocalDependency(Class<? extends T> clazz) {
        super(clazz);
    }

    @Override
    public T get() {
        return threadLocal.get();
    }
}
