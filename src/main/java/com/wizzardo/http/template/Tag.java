package com.wizzardo.http.template;

import com.wizzardo.tools.misc.Unchecked;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: moxa
 * Date: 7/3/13
 */
public abstract class Tag extends RenderableList {

    public Tag(Map<String, String> attrs, Body body) {
    }

    protected void prepareAttrs(Map<String, String> attrs) {
        for (Map.Entry<String, String> attr : attrs.entrySet()) {
            append(" ");
            ViewRenderer.prepare(attr.getKey(), this);
            append("=\"");
            ViewRenderer.prepare(attr.getValue(), this);
            append("\"");
        }
    }
}
