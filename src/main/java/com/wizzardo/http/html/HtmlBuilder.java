package com.wizzardo.http.html;

/**
 * Created by wizzardo on 08.01.15.
 */
public class HtmlBuilder extends Tag {

    protected Tag root;

    public HtmlBuilder() {
        body = new Body();
        body.add(new Tag.Doctype());
        root = new Tag("html");
        body.add(root);
    }

    @Override
    public Tag add(Tag tag) {
        return root.add(tag);
    }

    @Override
    public Tag attr(String name, String value) {
        return root.attr(name, value);
    }

    @Override
    public void render(Renderer renderer) {
        body.render(renderer);
    }

    public static Tag header() {
        return new Tag("header");
    }

    public static Tag head() {
        return new Tag("head");
    }

    public static Tag body() {
        return new Tag("body");
    }

    public static Tag p() {
        return new Tag("p");
    }

    public static Tag div() {
        return new Tag("div");
    }

    public static Tag br() {
        return new Tag("br");
    }

    public static Tag.Script script() {
        return new Tag.Script();
    }

    public static Tag.Link link() {
        return new Tag.Link();
    }

    public static Tag meta() {
        return new Tag("meta");
    }

    public static Tag table() {
        return new Tag("table");
    }

    public static Tag tr() {
        return new Tag("tr");
    }

    public static Tag td() {
        return new Tag("td");
    }

    public static Tag th() {
        return new Tag("th");
    }

    public static Tag strong() {
        return new Tag("strong");
    }

    public static Tag span() {
        return new Tag("span");
    }

    public static Tag title(String title) {
        return new Tag("title").text(title);
    }

    public static Tag h(int level) {
        return new Tag("h" + level);
    }

    public static Tag.A a() {
        return new Tag.A();
    }

    public static class Meta {
        private Meta() {
        }

        public static Tag charset(String charset) {
            return meta().attr("charset", charset);
        }

        public static Tag description(String content) {
            return meta().attr("name", "description").attr("content", content);
        }

        public static Tag keywords(String content) {
            return meta().attr("name", "keywords").attr("content", content);
        }

        public static Tag author(String content) {
            return meta().attr("name", "author").attr("content", content);
        }
    }
}
