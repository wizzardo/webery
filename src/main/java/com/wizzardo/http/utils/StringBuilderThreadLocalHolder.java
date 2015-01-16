package com.wizzardo.http.utils;

import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
import com.wizzardo.tools.misc.SoftThreadLocal;

/**
 * Created by wizzardo on 16.01.15.
 */
public class StringBuilderThreadLocalHolder {

    protected final SoftThreadLocal<ExceptionDrivenStringBuilder> stringBuilder = new SoftThreadLocal<ExceptionDrivenStringBuilder>() {
        @Override
        protected ExceptionDrivenStringBuilder init() {
            return new ExceptionDrivenStringBuilder();
        }

        @Override
        public ExceptionDrivenStringBuilder getValue() {
            ExceptionDrivenStringBuilder builder = super.getValue();
            builder.setLength(0);
            return builder;
        }
    };

    public ExceptionDrivenStringBuilder get() {
        return stringBuilder.getValue();
    }
}
