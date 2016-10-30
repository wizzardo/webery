package com.wizzardo.http.framework.parameters;

import java.lang.annotation.*;

/**
 * Created by wizzardo on 16/10/16.
 */

@Target(value = {ElementType.PARAMETER, ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Parameter {
    String name() default "";

    String def() default "";
}
