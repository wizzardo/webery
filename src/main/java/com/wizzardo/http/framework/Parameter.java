package com.wizzardo.http.framework;

import java.lang.annotation.*;

/**
 * Created by wizzardo on 16/10/16.
 */

@Target(value = ElementType.PARAMETER)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Parameter {
    String name();

    String def() default "";
}
