package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;

import java.util.Collection;
import java.util.Map;

/**
 * Created by wizzardo on 12.05.15.
 */
public class Collect extends Tag {

    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder<Collection> in = new ExpressionHolder<>(remove(attrs, "in"));
        ExpressionHolder expr = new ExpressionHolder<>(remove(attrs, "expr"));

        add(model -> {
            Collection src = in.getRaw(model);
            RenderResult result = new RenderResult();
            for (Object ob : src) {
                model.put("it", ob);
                model.put("it", expr.getRaw(model));
                result.add(body.get(model));
            }
            return result;
        });
        return this;
    }
}
