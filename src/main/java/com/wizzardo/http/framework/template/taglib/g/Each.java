package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;

import java.util.Collection;
import java.util.Map;

/**
 * @author: moxa
 * Date: 7/3/13
 */
public class Each extends Tag {

    public Each(Map<String, String> attrs, final Body body, String offset) {
        super(attrs, body, offset);

        ExpressionHolder<Collection> raw = new ExpressionHolder<>(attrs.get("in"));
        Object varName = attrs.get("var");
        String var = varName == null ? "it" : String.valueOf(varName);

        String indexName = attrs.get("status");

        add(model -> {
            Collection in = raw.getRaw(model);
            RenderResult result = new RenderResult();
            int i = 0;
            for (Object ob : in) {
                model.put(var, ob);
                if (indexName != null)
                    model.put(indexName, i++);

                result.add(body.get(model));
            }
            return result;
        });
    }
}