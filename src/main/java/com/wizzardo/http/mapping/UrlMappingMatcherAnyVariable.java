package com.wizzardo.http.mapping;

import com.wizzardo.http.Named;

import java.util.function.BiConsumer;

/**
* Created by wizzardo on 27.03.15.
*/
class UrlMappingMatcherAnyVariable<T extends Named> extends UrlMappingWithVariables<T> {
    protected String variable;

    protected UrlMappingMatcherAnyVariable(UrlMapping<T> parent, String part, int partNumber) {
        super(parent, part, partNumber);
        variable = variables[0];
    }

    @Override
    protected boolean matches(String urlPart) {
        return true;
    }

    @Override
    protected void prepare(BiConsumer<String, String> parameterConsumer, Path path) {
        if (parent != null)
            parent.prepare(parameterConsumer, path);

        String part = path.getPart(partNumber);

        if (part != null)
            parameterConsumer.accept(variable, part);
    }
}
