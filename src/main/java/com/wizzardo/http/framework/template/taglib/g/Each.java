package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.Tag;
import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.RenderResult;

import java.util.Collection;
import java.util.Map;

/**
 * @author: moxa
 * Date: 7/3/13
 */
public class Each extends Tag {

    public Each(Map<String, String> attrs, final Body body, String offset) {
        super(attrs, body, offset);

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