package com.wizzardo.http.framework.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0)
            return () -> newInstance(clazz);

        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 0)
                return () -> newInstance((Constructor<T>) constructor);
        }

        Constructor<T> constructor = (Constructor<T>) constructors[0];
        Class<?>[] argsTypes = constructor.getParameterTypes();
        return () -> {
            Object[] args = new Object[argsTypes.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = DependencyFactory.get(argsTypes[i]);
            }
            return newInstance(constructor, args);
        };
    }

    static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("can't create instance of class " + clazz, e);
        }
    }

    static <T> T newInstance(Constructor<T> constructor) {
        try {
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("can't create instance of class " + constructor.getDeclaringClass(), e);
        }
    }

    static <T> T newInstance(Constructor<T> constructor, Object[] args) {
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("can't create instance of class " + constructor.getDeclaringClass(), e);
        }
    }
}
