package com.wizzardo.http.framework.template;

import com.wizzardo.tools.xml.Node;

import java.util.List;
import java.util.Map;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public class Body extends RenderableList {
    private Node node;

    public Body(Node node, String offset, String dir, List<String> imports) {
        this(node, offset, dir, true, imports);
    }

    public Body(Node node, String offset, String dir, boolean addNewLine, List<String> imports) {
        this.node = node;

        for (Node child : node.children()) {
            ViewRenderer.prepare(child, this, dir, offset, addNewLine, imports);
        }
    }

    public Map<String, String> attributes() {
        return node.attributes();
    }
}