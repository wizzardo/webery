package com.wizzardo.http.framework.di;

import java.util.function.Supplier;

/**
 * Created by wizzardo on 27/03/17.
 */
public interface DependencyForge {
    default <T> Dependency<? extends T> forge(Class<? extends T> clazz, DependencyScope scope) {
        return forge(clazz, createSupplier(clazz), scope);
    }

    default <T> Dependency<? extends T> forge(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope) {
        if (supplier == null)
            return null;

        return scope.forge(clazz, supplier, scope);
    }

    default <T> Supplier<T> createSupplier(Class<? extends T> clazz) {
        return () -> Dependency.newInstance(clazz);
    }
}
