package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.*;

import java.util.Map;

/**
 * Created by wizzardo on 08.05.15.
 */
public class Else extends Tag {

    protected final Body body;
    protected Else elseTag;

    public Else(Map<String, String> attrs, Body body, String offset) {
        super(attrs, body, offset);
        this.body = body;
    }

    @Override
    public void appendTo(RenderableList l) {
        Renderable tag = l.get(l.size() - 1);
        if (!If.class.isAssignableFrom(tag.getClass()))
            throw new IllegalStateException("If tag must be before Else tag");

        ((If) tag).setElse(this);
    }

    @Override
    public RenderResult get(Map<String, Object> model) {
        RenderResult result = new RenderResult();
        result.add(body.get(model));
        return result;
    }
}
