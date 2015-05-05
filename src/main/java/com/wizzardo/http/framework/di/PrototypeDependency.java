package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 05.05.15.
 */
public class PrototypeDependency<T> extends Dependency<T> {
    protected Class<T> clazz;

    PrototypeDependency(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T get() {
        try {
            T instance = clazz.newInstance();
            injectDependencies(instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("can't create instance of class " + clazz, e);
        }
    }
}
