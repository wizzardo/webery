package com.wizzardo.http.framework.di;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.reflection.FieldReflection;
import com.wizzardo.tools.reflection.FieldReflectionFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by wizzardo on 05.05.15.
 */
public abstract class Dependency<T> {
    protected static Cache<Class, List<FieldReflection>> dependencies = new Cache<>(0, clazz -> {
        List<FieldReflection> l = new ArrayList<>();
        FieldReflectionFactory reflectionFactory = new FieldReflectionFactory();
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                int mod = f.getType().getModifiers();
                if (Modifier.isFinal(mod) || f.getType().isPrimitive())
                    continue;
                if (f.getType().getAnnotation(Injectable.class) != null
                        || Modifier.isAbstract(mod)
                        || Modifier.isInterface(mod)) {
                    l.add(reflectionFactory.create(f, true));
                } else
                    for (Class i : f.getType().getInterfaces()) {
                        if (i.getAnnotation(Injectable.class) != null) {
                            l.add(reflectionFactory.create(f, true));
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
        try {
            for (FieldReflection f : dependencies.get(ob.getClass())) {
                f.setObject(ob, DependencyFactory.getDependency(f.getField().getType()));
            }
        } catch (Exception e) {
            synchronized (ob.getClass()) {
                List<FieldReflection> list = new ArrayList<>(dependencies.get(ob.getClass()));
                Iterator<FieldReflection> iterator = list.iterator();

                while (iterator.hasNext()) {
                    FieldReflection f = iterator.next();
                    try {
                        f.setObject(ob, DependencyFactory.getDependency(f.getField().getType()));
                    } catch (Exception ex) {
                        iterator.remove();
                    }
                }
                dependencies.put(ob.getClass(), list);
            }
        }
    }
}
