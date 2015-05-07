package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 05.05.15.
 */
public class SingletonDependency<T> extends Dependency<T> {
    private T instance;
    private volatile boolean init = false;

    public SingletonDependency(Class<T> clazz) {
        try {
            this.instance = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ignored) {
            throw new IllegalStateException("can't create instance of class " + clazz);
        }
    }

    public SingletonDependency(T instance) {
        this(instance, false);
    }

    public SingletonDependency(T instance, boolean injectDependencies) {
        this.instance = instance;
        init = !injectDependencies;
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
