package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.Renderable;
import com.wizzardo.http.framework.template.RenderableList;
import com.wizzardo.http.framework.template.Tag;

import java.util.Map;

/**
 * Created by wizzardo on 08.05.15.
 */
public class Elseif extends If {

    public Elseif(Map<String, String> attrs, Body body, String offset) {
        super(attrs, body, offset);
    }

    @Override
    public void appendTo(RenderableList l) {
        Renderable tag = l.get(l.size() - 1);
        Tag elseTag;
        while (tag instanceof If && (elseTag = ((If) tag).getElse()) != null) {
            tag = elseTag;
        }
        if (!(tag instanceof If))
            throw new IllegalStateException("If tag must be before Else tag");

        ((If) tag).setElse(this);
    }
}
