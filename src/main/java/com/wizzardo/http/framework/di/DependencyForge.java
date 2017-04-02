package com.wizzardo.http.framework.di;

import java.util.function.Supplier;

/**
 * Created by wizzardo on 27/03/17.
 */
public interface DependencyForge {
    default <T> Dependency<? extends T> forge(Class<? extends T> clazz, DependencyScope scope) {
        return forge(clazz, () -> Dependency.newInstance(clazz), scope);
    }

    <T> Dependency<? extends T> forge(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope);
}
