package com.wizzardo.http.framework.template;

import com.wizzardo.tools.xml.Node;

/**
 * Created by wizzardo on 30.05.15.
 */
public interface Decorator {
    void decorate(Node from, Node to);
}
