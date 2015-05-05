package com.wizzardo.http.framework.di;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.cache.Computable;

import java.lang.annotation.Annotation;
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
    private Map<Class, Class> mapping = new HashMap<Class, Class>();

    @SuppressWarnings("unchecked")
    private Cache<Class, Dependency> dependencies = new Cache<Class, Dependency>(0, new Computable<Class, Dependency>() {
        @Override
        public Dependency compute(Class clazz) {

            Injectable injectable = (Injectable) getAnnotation(clazz, Injectable.class);
            if (injectable != null) {
                switch (injectable.scope()) {
                    case SINGLETON: {
                        return new SingletonDependency(clazz);
                    }
                    case PROTOTYPE:
                        return new PrototypeDependency(clazz);
                    case SESSION:
                        return new SessionDependency(clazz);
                }
            }

            if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers())) {
                Class implementation = mapping.get(clazz);
                if (implementation == null)
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
                if (implementation != null) {

                    if (injectable == null)
                        injectable = (Injectable) getAnnotation(implementation, Injectable.class);

                    if (injectable != null) {
                        switch (injectable.scope()) {
                            case SINGLETON: {
                                return new SingletonDependency(implementation);
                            }
                            case PROTOTYPE:
                                return new PrototypeDependency(implementation);
                            case SESSION:
                                return new SessionDependency(implementation);
                        }
                    } else
                        return new PrototypeDependency(implementation);
                }
            }

            if (Service.class.isAssignableFrom(clazz)) {
                return new SingletonDependency(clazz);
            }

            throw new IllegalStateException("can't create dependency-holder for class: " + clazz);
        }
    });

    private Annotation getAnnotation(Class clazz, Class annotation) {
        while (clazz != null) {
            Annotation a = clazz.getAnnotation(annotation);
            if (a != null) {
                return a;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static class DependencyFactoryHolder {
        private static final DependencyFactory instance = new DependencyFactory();
    }

    public static <T> T getDependency(Class<T> clazz) {
        return DependencyFactoryHolder.instance.resolveDependency(clazz);
    }

    public static DependencyFactory get() {
        return DependencyFactoryHolder.instance;
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveDependency(Class<T> clazz) {
        return (T) dependencies.get(clazz).get();
    }

    public void setClasses(List<Class> classes) {
        this.classes = classes;
    }

    public void bind(Class abstractClass, Class implementation) {
        mapping.put(abstractClass, implementation);
    }

    public <T> void register(Class<T> clazz, Dependency<T> dependency) {
        dependencies.put(clazz, dependency);
    }
}
