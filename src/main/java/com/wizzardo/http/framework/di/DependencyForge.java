package com.wizzardo.http.framework.di;

/**
 * Created by wizzardo on 27/03/17.
 */
public interface DependencyForge {
    <T> Dependency<? extends T> forge(Class<T> clazz);
}
