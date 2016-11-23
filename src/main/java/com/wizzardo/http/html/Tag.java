package com.wizzardo.http.html;

import com.wizzardo.tools.misc.Unchecked;

import java.util.*;
import java.util.concurrent.Callable;

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

    public interface ClosureVoidTwoArgs<A, B> {
        void execute(A a, B b);
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

    public <T> Tag each(Collection<T> collection, ClosureVoidTwoArgs<T, Tag> closure) {
        if (collection != null)
            for (T t : collection) {
                closure.execute(t, this);
            }
        return this;
    }

    public <T> Tag each(T[] collection, ClosureVoidTwoArgs<T, Tag> closure) {
        if (collection != null)
            for (T t : collection) {
                closure.execute(t, this);
            }
        return this;
    }

    public Tag addIf(boolean check, Callable<Tag> closure) {
        if (check)
            add(Unchecked.call(closure));
        return this;
    }

    public Tag addIf(boolean check, Tag tag) {
        if (check)
            add(tag);
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

        public Tag get(int i) {
            return tags.get(i);
        }

        public int size() {
            return tags.size();
        }
    }

    static class Doctype extends Tag {

        @Override
        public void render(Renderer renderer) {
            renderer.append("<!DOCTYPE html>");
        }
    }

    public static class Text extends Tag {
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
            body = new Body();
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

    public static class Form extends Tag {
        public Form() {
            super("form");
        }

        public Form method(String method) {
            attr("method", method);
            return this;
        }

        public Form action(String action) {
            attr("action", action);
            return this;
        }

        public Form enctype(String enctype) {
            attr("enctype", enctype);
            return this;
        }
    }

    public static class Select extends Tag {
        public Select(List list) {
            super("select");
            for (Object o : list) {
                String s = String.valueOf(o);
                add(new Tag("option").attr("value", s).text(s));
            }
        }

        public Select(Map<?, ?> map) {
            super("select");
            for (Map.Entry e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                String value = String.valueOf(e.getValue());
                add(new Tag("option").attr("value", key).text(value));
            }
        }
    }

    public static class Input extends Tag {
        public Input() {
            super("input");
        }

        public Input type(String type) {
            attr("type", type);
            return this;
        }

        public Input name(String name) {
            attr("name", name);
            return this;
        }

        public Input value(String value) {
            attr("value", value);
            return this;
        }

        public Input placeholder(String placeholder) {
            attr("placeholder", placeholder);
            return this;
        }
    }
}
