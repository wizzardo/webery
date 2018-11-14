package com.wizzardo.http.framework.di;

import java.util.function.Supplier;

/**
 * Created by wizzardo on 05.05.15.
 */
public class SingletonDependency<T> extends Dependency<T> {
    private T instance;
    private boolean injecting = false;
    private volatile boolean init = false;

    public SingletonDependency(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope) {
        super(clazz, supplier, scope);
    }

    public SingletonDependency(T instance) {
        this(instance, false);
    }

    public SingletonDependency(Class<? extends T> clazz) {
        super(clazz, DependencyScope.SINGLETON);
    }

    public SingletonDependency(T instance, boolean injectDependencies) {
        super((Class<? extends T>) instance.getClass(), () -> instance, DependencyScope.SINGLETON);
        this.instance = instance;
        init = !injectDependencies;
    }

    @Override
    public T get() {
        if (!init) {
            synchronized (this) {
                if (!init) {
                    if (!injecting) {
                        if (instance == null)
                            instance = newInstance(false);

                        injecting = true;
                        injectDependencies(instance);
                        injecting = false;
                        onCreate(instance);
                    }
                    init = true;
                }
            }
        }
        return instance;
    }
}
