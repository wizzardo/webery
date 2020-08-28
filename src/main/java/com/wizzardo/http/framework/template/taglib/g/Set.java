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

    public Tag init(Map<String, String> attrs, Body body, String offset) {
        String var = remove(attrs, "var");

        ExpressionHolder raw = asExpression(attrs, "value", false, true);
        add((model, result) -> {
            model.put(var, raw.getRaw(model));
            return result;
        });
        return this;
    }
}
