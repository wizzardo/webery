package com.wizzardo.http.framework.di;

import com.wizzardo.http.Session;
import com.wizzardo.http.framework.RequestContext;

/**
 * Created by wizzardo on 05.05.15.
 */
public class SessionDependency<T> extends Dependency<T> {
    protected Class<T> clazz;

    public SessionDependency(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T get() {
        Session session = ((RequestContext) Thread.currentThread()).getRequestHolder().request.session();
        T t = (T) session.get(clazz);
        if (t == null)
            session.put(clazz, t = newInstance(clazz));

        return t;
    }
}
