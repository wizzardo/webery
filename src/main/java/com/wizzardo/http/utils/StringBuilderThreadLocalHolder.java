package com.wizzardo.http.utils;

import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
import com.wizzardo.tools.misc.SoftThreadLocal;

/**
 * Created by wizzardo on 16.01.15.
 */
public class StringBuilderThreadLocalHolder {

    protected final SoftThreadLocal<ExceptionDrivenStringBuilder> stringBuilder = new SoftThreadLocal<>(
            ExceptionDrivenStringBuilder::new,
            ExceptionDrivenStringBuilder::clear
    );

    public ExceptionDrivenStringBuilder get() {
        return stringBuilder.getValue();
    }
}
