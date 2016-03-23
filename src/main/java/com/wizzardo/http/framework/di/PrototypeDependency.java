package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 05.05.15.
 */
public class PrototypeDependency<T> extends Dependency<T> {

    public PrototypeDependency(Class<? extends T> clazz) {
        super(clazz);
    }

    @Override
    public T get() {
        return newInstance();
    }
}
