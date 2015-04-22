package com.wizzardo.http.mapping;

import com.wizzardo.http.request.Request;

/**
* Created by wizzardo on 27.03.15.
*/
class UrlMappingMatcherAnyVariable<T> extends UrlMappingWithVariables<T> {
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
    protected void prepare(Request request) {
        if (request == null)
            return;

        if (parent != null)
            parent.prepare(request);

        String part = request.path().getPart(partNumber);

        if (part != null)
            request.param(variable, part);
    }
}
