package com.wizzardo.http.html;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 08.01.15.
 */
public class Tag {
    protected Map<String, String> attrs;
    protected String name;
    protected Body body;

    Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public void render(Renderer renderer) {
        renderer.append('<').append(name);
        if (attrs != null) {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                renderer.append(' ').append(entry.getKey()).append("=\"").append(entry.getValue()).append('"');
            }
        }

        if (body == null) {
            renderer.append("/>");
        } else {
            renderer.append(">");
            body.render(renderer);
            renderer.append("</").append(name).append('>');
        }
    }

    public Tag add(Tag tag) {
        if (body == null)
            body = new Body();
        body.add(tag);
        return this;
    }

    public Tag attr(String name, String value) {
        if (attrs == null)
            attrs = new LinkedHashMap<>();
        attrs.put(name, value);
        return this;
    }

    public Tag text(String text) {
        return add(new Tag.Text(text));
    }

    public static class Body {
        private List<Tag> tags = new ArrayList<>();

        public void add(Tag tag) {
            tags.add(tag);
        }

        public void render(Renderer renderer) {
            for (Tag tag : tags)
                tag.render(renderer);
        }
    }

    static class Doctype extends Tag {

        @Override
        public void render(Renderer renderer) {
            renderer.append("<!DOCTYPE html>");
        }
    }

    static class Text extends Tag {
        public Text(String text) {
            super(text);
        }

        @Override
        public void render(Renderer renderer) {
            renderer.append(name);
        }

        @Override
        public Tag attr(String name, String value) {
            throw new IllegalStateException("Text tag can not have any attributes");
        }

        @Override
        public Tag add(Tag tag) {
            throw new IllegalStateException("Text tag can not have any inner tags");
        }
    }
}
