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
import java.util.function.Supplier;

import static com.wizzardo.http.framework.di.DependencyFactory.hasAnnotation;

/**
 * Created by wizzardo on 05.05.15.
 */
public abstract class Dependency<T> {
    protected final Class<? extends T> clazz;
    protected final Supplier<T> supplier;
    protected final DependencyScope scope;

    public Dependency(Class<? extends T> clazz, DependencyScope scope) {
        this(clazz, scope.createSupplier(clazz), scope);
    }

    public Dependency(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope) {
        this.scope = scope;
        this.supplier = supplier;
        this.clazz = clazz;
    }

    protected static Cache<Class, List<FieldReflection>> dependencies = new Cache<>("class-fields", 0, clazz -> {
        ArrayList<FieldReflection> l = new ArrayList<>();
        FieldReflectionFactory reflectionFactory = new FieldReflectionFactory();
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                if (Modifier.isFinal(f.getModifiers()))
                    continue;

                l.add(reflectionFactory.create(f, true));
            }
            clazz = clazz.getSuperclass();
        }
        l.trimToSize();
        return l;
    });

    public abstract T get();

    protected T newInstance() {
        return prepare(supplier.get(), true);
    }

    protected T newInstance(boolean injectDependenciesAndInit) {
        return prepare(supplier.get(), injectDependenciesAndInit);
    }

    protected T prepare(T t, boolean injectDependenciesAndInit) {
        if (injectDependenciesAndInit) {
            injectDependencies(t);
            onCreate(t);
        }
        return t;
    }

    protected void onCreate(T t) {
        if (t instanceof PostConstruct)
            ((PostConstruct) t).init();
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
