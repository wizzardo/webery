package com.wizzardo.http.mapping;

import com.wizzardo.http.request.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Created by wizzardo on 27.03.15.
*/
class UrlMappingWithVariables<T> extends UrlMappingMatcher<T> {
    String[] variables;
    Pattern pattern;
    int partNumber;

    UrlMappingWithVariables(UrlMapping<T> parent, String part, int partNumber) {
        super(parent);
        this.partNumber = partNumber;
        Matcher m = VARIABLES.matcher(part);
        final List<String> vars = new ArrayList<>();
        while (m.find()) {
            vars.add(m.group(1));
        }
        part = convertRegexpVariables(part);

        pattern = Pattern.compile(part);
        variables = vars.toArray(new String[vars.size()]);
    }

    @Override
    protected void prepare(Request request) {
        super.prepare(request);
        if (request == null)
            return;

        String part = request.path().getPart(partNumber);
        if (part == null)
            return;
        Matcher matcher = pattern.matcher(part);
        if (matcher.find()) {
            for (int i = 1; i <= variables.length; i++) {
                request.param(variables[i - 1], matcher.group(i));
            }
        }
    }

    @Override
    protected boolean matches(String urlPart) {
        return pattern.matcher(urlPart).matches();
    }

    @Override
    protected void setValue(T t) {
        super.setValue(t);
        if (parent != null && pattern.pattern().equals(OPTIONAL))
            parent.setValue(t);
    }
}
