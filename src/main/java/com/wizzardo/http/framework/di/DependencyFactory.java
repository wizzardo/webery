package com.wizzardo.http.framework.di;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.cache.Computable;

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

    @SuppressWarnings("unchecked")
    private Cache<Class, Dependency> dependencies = new Cache<>(0, new Computable<Class, Dependency>() {
        @Override
        public Dependency compute(Class clazz) {

            Injectable injectable = (Injectable) getAnnotation(clazz, Injectable.class);
            if (injectable != null)
                return injectable.scope().createDependency(clazz);

            if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers())) {
                Class implementation = mappingByClass.get(clazz);
                if (implementation == null) {
                    for (Class cl : classes) {
                        if (clazz.isAssignableFrom(cl)
                                && !Modifier.isInterface(cl.getModifiers())
                                && !Modifier.isAbstract(cl.getModifiers())
                                && (injectable = (Injectable) getAnnotation(cl, Injectable.class)) != null
                                ) {
                            if (implementation != null) {
                                throw new IllegalStateException("can't resolve dependency '" + clazz + "'. Found more than one implementation: " + implementation + " and " + cl);
                            }
                            implementation = cl;
                        }
                    }
                }
                if (implementation != null) {
                    if (injectable == null)
                        injectable = (Injectable) getAnnotation(implementation, Injectable.class);

                    if (injectable != null)
                        return injectable.scope().createDependency(implementation);
                    else
                        return new PrototypeDependency(implementation);
                }
            }

            if (Service.class.isAssignableFrom(clazz)) {
                return new SingletonDependency(clazz);
            }

            throw new IllegalStateException("can't create dependency-holder for class: " + clazz);
        }
    });

    static Annotation getAnnotation(Class clazz, Class annotation) {
        while (clazz != null) {
            Annotation a = clazz.getAnnotation(annotation);
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

    public boolean contains(Class clazz) {
        return dependencies.contains(clazz);
    }
}
