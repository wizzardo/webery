package com.wizzardo.http.framework.template;

import com.wizzardo.tools.xml.Node;

import java.util.List;

/**
 * Created by wizzardo on 30.05.15.
 */
public interface Decorator {
    void decorate(Node from, Node to);

    /**
     * replace target with children of replacement
     */
    default void replace(Node target, Node replacement) {
        if (target == null)
            return;

        List<Node> nodes = target.parent().children();
        int i = nodes.indexOf(target);
        nodes.remove(i);
        if (replacement != null)
            nodes.addAll(i, replacement.children());
    }
}
