package com.wizzardo.http.framework.di;

import com.wizzardo.tools.cache.Cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: moxa
 * Date: 7/22/13
 */
public class DependencyFactory {

    public interface AnnotationDependencyResolver<A extends Annotation> {
        <T> Dependency<T> resolve(A annotation, Class<T> clazz);
    }

    private List<Class> classes;
    private final Map<Class, Class> mappingByClass = new ConcurrentHashMap<>();
    private final Map<String, Dependency> mappingByName = new ConcurrentHashMap<>();
    private final Map<Class<? extends Annotation>, AnnotationDependencyResolver> annotationDependencyResolvers = new ConcurrentHashMap<>();

    private Cache<Class, Dependency> dependencies = new Cache<>("dependencies", 0, clazz -> {
        Injectable injectable = getAnnotation(clazz, Injectable.class);
        Class mappedClass = mappingByClass.get(clazz);
        boolean mapped = mappedClass != null && clazz != mappedClass;

        if (injectable != null && !isAbstract(clazz) && !mapped)
            return getForge(injectable).forge(clazz, injectable.scope());

        Class<?> implementation = findImplementation(clazz);
        if (implementation != null) {
            if (injectable == null)
                injectable = getAnnotation(implementation, Injectable.class);

            if (injectable != null)
                return getForge(injectable).forge(implementation, injectable.scope());
        }

        if (injectable != null) {
            Dependency dependency = getForge(injectable).forge(clazz, injectable.scope());
            if (dependency != null) {
                return dependency;
            }
        }

        throw new IllegalStateException("can't create dependency-holder for class: " + clazz);
    });

    public <A extends Annotation> void addResolver(Class<A> type, AnnotationDependencyResolver<A> annotationDependencyResolver) {
        annotationDependencyResolvers.put(type, annotationDependencyResolver);
    }

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

    protected DependencyForge getForge(Injectable injectable) {
        Class<? extends DependencyForge> forge = injectable.forge();
        if (!DependencyForge.class.equals(forge))
            return resolve(forge);
        else
            return injectable.scope();
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

        return get((Class<T>) field.getType(), field.getDeclaredAnnotations());
    }

    public static <T> T get(Class<T> type, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            AnnotationDependencyResolver resolver = get().annotationDependencyResolvers.get(annotation.annotationType());
            if (resolver != null)
                return (T) resolver.resolve(annotation, type).get();
        }

        return get(type);
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

    public boolean contains(String clazz) {
        return mappingByName.containsKey(clazz);
    }
}
