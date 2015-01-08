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

    public static Tag body() {
        return new Tag("body");
    }

    public static Tag p() {
        return new Tag("p");
    }

    public static Tag text(String text) {
        return new Tag.Text(text);
    }
}
