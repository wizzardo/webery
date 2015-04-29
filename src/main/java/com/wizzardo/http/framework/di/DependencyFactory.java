package com.wizzardo.http.framework.di;

import com.wizzardo.http.Session;
import com.wizzardo.http.framework.WebWorker;
import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.cache.Computable;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

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

    void setClasses(List<Class> classes) {
        this.classes = classes;
    }

    public void addClassMapping(Class abstractClass, Class implementation) {
        mapping.put(abstractClass, implementation);
    }

    private static abstract class Dependency<T> {
        private static Cache<Class, List<Field>> dependencies = new Cache<Class, List<Field>>(0, clazz -> {
            List<Field> l = new ArrayList<Field>();
            while (clazz != null) {
                for (Field f : clazz.getDeclaredFields()) {
                    int mod = f.getType().getModifiers();
                    if (Modifier.isFinal(mod) || f.getType().isPrimitive())
                        continue;
                    if (f.getType().getAnnotation(Injectable.class) != null
                            || Modifier.isAbstract(mod)
                            || Modifier.isInterface(mod)) {
                        f.setAccessible(true);
                        l.add(f);
                    } else
                        for (Class i : f.getType().getInterfaces()) {
                            if (i.getAnnotation(Injectable.class) != null) {
                                f.setAccessible(true);
                                l.add(f);
                                break;
                            }
                        }
                }
                clazz = clazz.getSuperclass();
            }
            return l;
        });

        public abstract T get();

        protected void injectDependencies(Object ob) {
            System.out.println("injectDependencies for " + ob);
            Iterator<Field> iter = dependencies.get(ob.getClass()).iterator();
            while (iter.hasNext()) {
                Field f = iter.next();
                try {
                    System.out.println("trying inject " + f.getName() + " (" + f.getType() + ")");
                    f.set(ob, DependencyFactory.getDependency(f.getType()));
                    System.out.println("\tsuccess");
                } catch (IllegalAccessException | IllegalStateException | NullPointerException ignored) {
                    iter.remove();
                    System.out.println("\tfailed");
                }

            }
        }
    }

    private static class SingletonDependency<T> extends Dependency<T> {
        private T instance;
        private volatile boolean init = false;

        private SingletonDependency(Class<T> clazz) {
            try {
                this.instance = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ignored) {
                throw new IllegalStateException("can't create instance of class " + clazz);
            }
        }

        @Override
        public T get() {
            if (!init) {
                synchronized (this) {
                    if (!init) {
                        init = true;
                        injectDependencies(instance);
                    }
                }
            }
            return instance;
        }
    }

    private static class PrototypeDependency<T> extends Dependency<T> {
        private Class<T> clazz;

        private PrototypeDependency(Class<T> clazz) {
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

    private static class SessionDependency<T> extends Dependency<T> {
        private Class<T> clazz;

        private SessionDependency(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T get() {
            Session session = ((WebWorker) Thread.currentThread()).getRequestHolder().request.session();
            Object t = session.get(clazz);
            if (t == null) {
                try {
                    t = clazz.newInstance();
                    injectDependencies(t);
                    session.put(clazz, t);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException("can't create instance of class " + clazz, e);
                }
            }
            return (T) t;
        }
    }
}
