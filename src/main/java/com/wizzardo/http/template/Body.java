package com.wizzardo.http.template;

import com.wizzardo.tools.xml.Node;

import java.util.Map;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public class Body extends RenderableList {
    private Node node;

    public Body(Node node, String offset, String dir) {
        this.node = node;

        for (Node child : node.children()) {
            ViewRenderer.prepare(child, this, dir, offset + ViewRenderer.OFFSET);
        }
    }

    public Map<String, String> attributes() {
        return node.attributes();
    }
}