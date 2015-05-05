package com.wizzardo.http.framework.di;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.reflection.FieldReflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wizzardo on 05.05.15.
 */
public abstract class Dependency<T> {
    protected static Cache<Class, List<FieldReflection>> dependencies = new Cache<>(0, clazz -> {
        List<FieldReflection> l = new ArrayList<>();
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                int mod = f.getType().getModifiers();
                if (Modifier.isFinal(mod) || f.getType().isPrimitive())
                    continue;
                if (f.getType().getAnnotation(Injectable.class) != null
                        || Modifier.isAbstract(mod)
                        || Modifier.isInterface(mod)) {
                    l.add(new FieldReflection(f, true));
                } else
                    for (Class i : f.getType().getInterfaces()) {
                        if (i.getAnnotation(Injectable.class) != null) {
                            l.add(new FieldReflection(f, true));
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
        for (FieldReflection f : dependencies.get(ob.getClass())) {
            f.setObject(ob, DependencyFactory.getDependency(f.getField().getType()));
        }
    }
}
