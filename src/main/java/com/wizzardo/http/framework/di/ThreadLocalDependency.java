package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 05.05.15.
 */
public class ThreadLocalDependency<T> extends Dependency<T> {
    protected Class<T> clazz;
    protected ThreadLocal<T> threadLocal = new ThreadLocal<T>() {
        @Override
        protected T initialValue() {
            return newInstance(clazz);
        }
    };

    public ThreadLocalDependency(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T get() {
        return threadLocal.get();
    }
}
