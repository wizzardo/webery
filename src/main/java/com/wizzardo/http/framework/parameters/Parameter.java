package com.wizzardo.http.framework.parameters;

import java.lang.annotation.*;

/**
 * Created by wizzardo on 16/10/16.
 */

@Target(value = {ElementType.PARAMETER, ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Parameter {

    class Constants {
        final static String DEFAULT_NONE = "\r\n";
    }

    String name() default "";

    String def() default Constants.DEFAULT_NONE;
}
