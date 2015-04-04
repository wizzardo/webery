package com.wizzardo.http.html;

import com.wizzardo.tools.collections.CollectionTools;

import java.util.*;

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

    public <T> Tag each(Collection<T> collection, CollectionTools.Closure<Tag, T> closure) {
        if (collection != null)
            for (T t : collection) {
                add(closure.execute(t));
            }
        return this;
    }

    public <T> Tag each(T[] collection, CollectionTools.Closure<Tag, T> closure) {
        if (collection != null)
            for (T t : collection) {
                add(closure.execute(t));
            }
        return this;
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

    public Tag id(String id) {
        return attr("id", id);
    }

    public Tag clazz(String clazz) {
        return attr("class", clazz);
    }

    public Tag style(String style) {
        return attr("style", style);
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

    public static class A extends Tag {
        public A() {
            super("a");
        }

        public A href(String url) {
            attr("href", url);
            return this;
        }
    }

    public static class Script extends Tag {
        public Script() {
            this("text/javascript");
        }

        public Script(String type) {
            super("script");
            attr("type", type);
        }

        public Script src(String url) {
            attr("src", url);
            return this;
        }
    }

    public static class Link extends Tag {
        public Link() {
            this("stylesheet");
        }

        public Link(String rel) {
            super("link");
            attr("rel", rel);
        }

        public Link href(String url) {
            attr("href", url);
            return this;
        }
    }
}
