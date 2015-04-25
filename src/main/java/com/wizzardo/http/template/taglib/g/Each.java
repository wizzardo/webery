package com.wizzardo.http.template.taglib.g;

import com.wizzardo.http.template.Body;
import com.wizzardo.http.template.ExpressionHolder;
import com.wizzardo.http.template.RenderResult;
import com.wizzardo.http.template.Tag;

import java.util.Collection;
import java.util.Map;

/**
 * @author: moxa
 * Date: 7/3/13
 */
public class Each extends Tag {

    public Each(Map<String, String> attrs, final Body body) {
        super(attrs, body);

        final ExpressionHolder raw = new ExpressionHolder(attrs.get("in"));
        Object varName = attrs.get("var");
        final String var = varName == null ? "it" : String.valueOf(varName);

        add(model -> {
            Collection in = (Collection) raw.getRaw(model);
            RenderResult result = new RenderResult();
            for (Object ob : in) {
                model.put(var, ob);
                result.add(body.get(model));
            }
            return result;
        });
    }
}