package com.wizzardo.http.framework.di;

import java.util.function.Supplier;

/**
 * Created by wizzardo on 05.05.15.
 */
public class PrototypeDependency<T> extends Dependency<T> {

    public PrototypeDependency(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope) {
        super(clazz, supplier, scope);
    }

    public PrototypeDependency(Class<? extends T> clazz) {
        super(clazz, DependencyScope.PROTOTYPE);
    }

    @Override
    public T get() {
        return newInstance();
    }
}
