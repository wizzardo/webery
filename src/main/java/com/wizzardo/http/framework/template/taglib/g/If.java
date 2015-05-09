package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Map;

/**
 * Created by wizzardo on 08.05.15.
 */
public class If extends Tag {

    protected Tag elseTag;
    protected final Body body;
    protected final ExpressionHolder exp;

    public If(Map<String, String> attrs, Body body, String offset) {
        super(attrs, body, offset);
        this.body = body;

        String test = attrs.remove("test");
        exp = new ExpressionHolder<>(test);
    }

    public void setElse(Tag elseTag) {
        this.elseTag = elseTag;
    }

    public Tag getElse() {
        return elseTag;
    }

    @Override
    public void appendTo(RenderableList l) {
        l.add(this);
    }

    @Override
    public RenderResult get(Map<String, Object> model) {
        RenderResult result = new RenderResult();
        if (AsBooleanExpression.toBoolean(exp.getRaw(model))) {
            result.add(body.get(model));
        } else if (elseTag != null) {
            result.add(elseTag.get(model));
        }
        return result;
    }
}
