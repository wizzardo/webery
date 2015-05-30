package com.wizzardo.http.framework.template.decorator;

import com.wizzardo.http.framework.template.Decorator;
import com.wizzardo.tools.xml.Node;

/**
 * Created by wizzardo on 30.05.15.
 */
public class LayoutHead implements Decorator {
    @Override
    public void decorate(Node from, Node to) {
        replace(to.find("g:layoutHead"), from.get("html/head"));
    }
}
