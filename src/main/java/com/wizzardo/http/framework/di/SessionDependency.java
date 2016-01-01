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
        Object t = session.get(clazz);
        if (t == null) {
            try {
                t = clazz.newInstance();
                injectDependencies(t);
                session.put(clazz, t);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("can't create instance of class " + clazz, e);
            }
        }
        return (T) t;
    }
}
