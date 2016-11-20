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

    public Tag init(Map<String, String> attrs, final Body body, String offset) {
        ExpressionHolder<Collection> raw = asExpression(attrs, "in", false, true);
        String var = attrs.getOrDefault("var", "it");

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
        return this;
    }

    @Override
    protected String getBodyOffset(String offset) {
        return offset;
    }
}