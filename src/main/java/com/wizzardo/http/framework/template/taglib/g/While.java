package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Collection;
import java.util.Map;

/**
 * Created by wizzardo on 16.05.15.
 */
public class While extends Tag {
    public While(Map<String, String> attrs, Body body, String offset) {
        super(attrs, body, offset);

        ExpressionHolder<Collection> raw = new ExpressionHolder<>(check(attrs, "test"));

        add(model -> {
            RenderResult result = new RenderResult();
            while (AsBooleanExpression.toBoolean(raw.getRaw(model))) {
                result.append(body.get(model));
            }
            return result;
        });
    }
}
