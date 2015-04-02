package com.wizzardo.http.html;

import org.junit.Assert;
import org.junit.Test;

import static com.wizzardo.http.html.HtmlBuilder.*;

/**
 * Created by wizzardo on 08.01.15.
 */
public class HtmlBuilderTest {

    @Test
    public void simpleTest() {
        HtmlBuilder html = new HtmlBuilder();
        html.add(header())
                .add(body()
                        .add(p().text("Hello, World!")));

        StringBuilder sb = new StringBuilder();
        html.render(Renderer.create(sb));

        Assert.assertEquals("<!DOCTYPE html><html><header/><body><p>Hello, World!</p></body></html>", sb.toString());
    }

    @Test
    public void attrs() {
        Tag tag = new Tag("a").attr("href", "http://example.com").attr("class", "example_link");

        StringBuilder sb = new StringBuilder();
        tag.render(Renderer.create(sb));

        Assert.assertEquals("<a href=\"http://example.com\" class=\"example_link\"/>", sb.toString());
    }

    @Test
    public void meta() {
        HtmlBuilder html = new HtmlBuilder();
        html.add(header()
                        .add(Meta.charset("UTF-8"))
                        .add(Meta.description("Free Web tutorials"))
                        .add(Meta.keywords("HTML,CSS,XML,JavaScript"))
                        .add(Meta.author("Hege Refsnes"))
        ).add(body());

        StringBuilder sb = new StringBuilder();
        html.render(Renderer.create(sb));

        Assert.assertEquals("<!DOCTYPE html>" +
                "<html>" +
                "<header>" +
                "<meta charset=\"UTF-8\"/>" +
                "<meta name=\"description\" content=\"Free Web tutorials\"/>" +
                "<meta name=\"keywords\" content=\"HTML,CSS,XML,JavaScript\"/>" +
                "<meta name=\"author\" content=\"Hege Refsnes\"/>" +
                "</header><body/></html>", sb.toString());
    }
}
