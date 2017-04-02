package com.wizzardo.http.framework.di;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.reflection.FieldReflection;
import com.wizzardo.tools.reflection.FieldReflectionFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.wizzardo.http.framework.di.DependencyFactory.hasAnnotation;

/**
 * Created by wizzardo on 05.05.15.
 */
public abstract class Dependency<T> {
    protected final Class<? extends T> clazz;
    protected final DependencyScope scope;

    public Dependency(Class<? extends T> clazz, DependencyScope scope) {
        this.scope = scope;
        this.clazz = clazz;
    }

    protected static Cache<Class, List<FieldReflection>> dependencies = new Cache<>("class-fields", 0, clazz -> {
        ArrayList<FieldReflection> l = new ArrayList<>();
        FieldReflectionFactory reflectionFactory = new FieldReflectionFactory();
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                int mod = f.getType().getModifiers();
                if (Modifier.isFinal(mod) || f.getType().isPrimitive())
                    continue;

                if (DependencyFactory.get().contains(f.getType())) {
                    l.add(reflectionFactory.create(f, true));
                    continue;
                }

                if (hasAnnotation(f.getType(), Injectable.class)
                        || Modifier.isAbstract(mod)
                        || Modifier.isInterface(mod)) {
                    l.add(reflectionFactory.create(f, true));
                } else {
                    for (Class i : f.getType().getInterfaces()) {
                        if (hasAnnotation(i, Injectable.class)) {
                            l.add(reflectionFactory.create(f, true));
                            break;
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        l.trimToSize();
        return l;
    });

    public abstract T get();

    protected T newInstance() {
        return newInstance(true);
    }

    protected T newInstance(boolean injectDependenciesAndInit) {
        try {
            T t = clazz.newInstance();
            if (injectDependenciesAndInit) {
                injectDependencies(t);
                if (t instanceof PostConstruct)
                    ((PostConstruct) t).init();
            }
            return t;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("can't create instance of class " + clazz, e);
        }
    }

    public static class InjectionFailedException extends RuntimeException {
        public InjectionFailedException(Throwable cause) {
            super(cause);
        }
    }

    protected void injectDependencies(Object ob) {
        boolean ok = true;
        Iterator<FieldReflection> i = dependencies.get(ob.getClass()).iterator();
        while (ok && i.hasNext()) {
            FieldReflection f = i.next();
            try {
                f.setObject(ob, DependencyFactory.get(f.getField()));
            } catch (InjectionFailedException e) {
                throw Unchecked.rethrow(e);
            } catch (Exception e) {
                if (hasAnnotation(f.getField().getType(), Injectable.class))
                    throw new InjectionFailedException(e);

                ok = false;
                synchronized (ob.getClass()) {
                    ArrayList<FieldReflection> list = new ArrayList<>(dependencies.get(ob.getClass()));
                    Iterator<FieldReflection> iterator = list.iterator();

                    while (iterator.hasNext()) {
                        f = iterator.next();
                        try {
                            f.setObject(ob, DependencyFactory.get(f.getField()));
                        } catch (Exception ex) {
                            if (hasAnnotation(f.getField().getType(), Injectable.class))
                                throw new InjectionFailedException(ex);

                            iterator.remove();
                        }
                    }
                    list.trimToSize();
                    dependencies.put(ob.getClass(), list);
                }
            }
        }
    }
}
