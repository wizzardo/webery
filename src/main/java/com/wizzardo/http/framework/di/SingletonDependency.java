package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 05.05.15.
 */
public class SingletonDependency<T> extends Dependency<T> {
    private T instance;
    private boolean injecting = false;
    private volatile boolean init = false;

    public SingletonDependency(Class<? extends T> clazz) {
        super(clazz);
    }

    public SingletonDependency(T instance) {
        this(instance, false);
    }

    public SingletonDependency(T instance, boolean injectDependencies) {
        super((Class<? extends T>) instance.getClass());
        this.instance = instance;
        init = !injectDependencies;
    }

    @Override
    public T get() {
        if (!init) {
            synchronized (this) {
                if (!init) {
                    if (!injecting) {
                        injecting = true;
                        if (instance == null)
                            instance = newInstance(false);

                        injectDependencies(instance);
                    }
                    init = true;
                }
            }
        }
        return instance;
    }
}
