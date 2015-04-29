package com.wizzardo.http.framework.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wizzardo on 27.04.15.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Injectable {
    DependencyScope scope() default DependencyScope.SINGLETON;
}