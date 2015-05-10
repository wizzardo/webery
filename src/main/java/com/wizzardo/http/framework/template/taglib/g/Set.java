package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;

import java.util.Map;

/**
 * Created by wizzardo on 08.05.15.
 */
public class Set extends Tag {

    public Set(Map<String, String> attrs, Body body, String offset) {
        super(attrs, body, offset);

        String var = attrs.get("var");
        if (var == null)
            throw new IllegalStateException("variable 'var' is mandatory");

        ExpressionHolder raw = new ExpressionHolder<>(attrs.get("value"));
        add(model -> {
            model.put(var, raw.get(model));
            return new RenderResult();
        });
    }
}
