package com.wizzardo.http.framework.template.decorator;

import com.wizzardo.http.framework.template.Decorator;
import com.wizzardo.tools.xml.Node;

import java.util.List;

/**
 * Created by wizzardo on 30.05.15.
 */
public class LayoutBody implements Decorator {
    @Override
    public void decorate(Node from, Node to) {
        replace(to.find("g:layoutBody"), from.get("html/body"));
    }
}
