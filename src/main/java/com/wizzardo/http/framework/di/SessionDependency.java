package com.wizzardo.http.framework.di;

import com.wizzardo.http.Session;
import com.wizzardo.http.framework.RequestContext;

import java.util.function.Supplier;

/**
 * Created by wizzardo on 05.05.15.
 */
public class SessionDependency<T> extends Dependency<T> {

    public SessionDependency(Class<? extends T> clazz, Supplier<T> supplier, DependencyScope scope) {
        super(clazz, supplier, scope);
    }

    @Override
    public T get() {
        Session session = ((RequestContext) Thread.currentThread()).getRequestHolder().request.session();
        T t = (T) session.get(clazz);
        if (t == null)
            session.put(clazz, t = newInstance());

        return t;
    }
}
