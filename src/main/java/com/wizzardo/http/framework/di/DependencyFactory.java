package com.wizzardo.http.framework.di;

import com.wizzardo.tools.cache.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: moxa
 * Date: 7/22/13
 */
public class DependencyFactory {

    private List<Class> classes;
    private Map<Class, Class> mappingByClass = new HashMap<>();
    private Map<String, Dependency> mappingByName = new HashMap<>();

    private Cache<Class<?>, Dependency> dependencies = new Cache<>("dependencies", 0, clazz -> {
        Injectable injectable = getAnnotation(clazz, Injectable.class);
        if (injectable != null && !isAbstract(clazz))
            return injectable.scope().createDependency(clazz);

        Class<?> implementation = findImplementation(clazz);
        if (implementation != null) {
            if (injectable == null)
                injectable = getAnnotation(implementation, Injectable.class);

            if (injectable != null)
                return injectable.scope().createDependency(implementation);
        }

        throw new IllegalStateException("can't create dependency-holder for class: " + clazz);
    });

    protected Class findImplementation(Class<?> interfase) {
        Class implementation = mappingByClass.get(interfase);
        if (implementation != null)
            return implementation;

        for (Class<?> cl : classes) {
            if (interfase.isAssignableFrom(cl) && !isAbstract(cl)) {
                if (implementation != null) {
                    throw new IllegalStateException("can't resolve dependency '" + interfase + "'. Found more than one implementation: " + implementation + " and " + cl);
                }
                implementation = cl;
            }
        }
        return implementation;
    }

    protected boolean isAbstract(Class clazz) {
        int modifiers = clazz.getModifiers();
        return Modifier.isInterface(modifiers) && Modifier.isAbstract(modifiers);
    }

    static <A extends Annotation> boolean hasAnnotation(Class clazz, Class<A> annotation) {
        return getAnnotation(clazz, annotation) != null;
    }

    static <A extends Annotation> A getAnnotation(Class<?> clazz, Class<A> annotation) {
        while (clazz != null) {
            A a = clazz.getAnnotation(annotation);
            if (a != null) {
                return a;
            }
            for (Class implemented : clazz.getInterfaces()) {
                a = getAnnotation(implemented, annotation);
                if (a != null)
                    return a;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public void clear() {
        dependencies.clear();
    }

    private static class DependencyFactoryHolder {
        private static final DependencyFactory instance = new DependencyFactory();
    }

    public static <T> T get(Field field) {
        Dependency<T> dependency = DependencyFactoryHolder.instance.mappingByName.get(field.getName());
        if (dependency != null)
            return dependency.get();

        return DependencyFactoryHolder.instance.resolve((Class<T>) field.getType());
    }

    public static <T> T get(Class<T> clazz) {
        return DependencyFactoryHolder.instance.resolve(clazz);
    }

    public static DependencyFactory get() {
        return DependencyFactoryHolder.instance;
    }

    @SuppressWarnings("unchecked")
    private <T> T resolve(Class<T> clazz) {
        return (T) dependencies.get(clazz).get();
    }

    public void setClasses(List<Class> classes) {
        this.classes = classes;
    }

    public void register(Class abstractClass, Class implementation) {
        mappingByClass.put(abstractClass, implementation);
        dependencies.get(abstractClass);
    }

    public <T> void register(Class<T> clazz, Dependency<T> dependency) {
        dependencies.put(clazz, dependency);
    }

    public <T> void register(String name, Dependency<T> dependency) {
        mappingByName.put(name, dependency);
    }

    public <T> void register(Class<T> clazz, T dependency) {
        dependencies.put(clazz, new SingletonDependency<>(dependency));
    }

    public <T> void register(String name, T dependency) {
        mappingByName.put(name, new SingletonDependency<>(dependency));
    }

    public boolean contains(Class clazz) {
        return dependencies.contains(clazz);
    }
}
