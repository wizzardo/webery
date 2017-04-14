package com.wizzardo.http.framework.di;

import com.wizzardo.http.framework.RequestContext;
import com.wizzardo.http.framework.RequestHolder;

import java.util.function.Supplier;

/**
 * Created by wizzardo on 05.05.15.
 */
public class RequestDependency<T> extends Dependency<T> {

    public RequestDependency(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope) {
        super(clazz, supplier, scope);
    }

    public RequestDependency(Class<? extends T> clazz) {
        super(clazz, DependencyScope.REQUEST);
    }

    @Override
    public T get() {
        RequestHolder requestHolder = ((RequestContext) Thread.currentThread()).getRequestHolder();
        T t = requestHolder.get(clazz);
        if (t == null)
            requestHolder.put(clazz, t = newInstance());

        return t;
    }
}
