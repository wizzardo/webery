package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Map;

/**
 * Created by wizzardo on 08.05.15.
 */
public class If extends Tag {

    protected Tag elseTag;
    protected Body body;
    protected ExpressionHolder exp;

    public Tag init(Map<String, String> attrs, Body body, String offset) {
        this.body = body;

        exp = asExpression(attrs, "test", false, true);
        return this;
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
    public RenderResult get(Map<String, Object> model, RenderResult into) {
        if (AsBooleanExpression.toBoolean(exp.getRaw(model))) {
            body.get(model, into);
        } else if (elseTag != null) {
            elseTag.get(model, into);
        }
        return into;
    }
}
