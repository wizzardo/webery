package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.RenderableList;
import com.wizzardo.http.framework.template.ViewRenderer;
import com.wizzardo.tools.xml.GspParser;
import com.wizzardo.tools.xml.Node;

/**
 * Created by wizzardo on 26.05.15.
 */
public interface TagTest {
    default RenderableList prepare(String html) {
        Node n = new GspParser().parse(html);
        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");
        return l;
    }

    ;
}
