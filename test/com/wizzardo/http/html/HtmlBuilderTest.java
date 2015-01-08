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
                        .add(p()
                                .add(text("Hello, World!"))));

        StringBuilder sb = new StringBuilder();
        html.render(Renderer.create(sb));

        Assert.assertEquals("<!DOCTYPE html><html><header/><body><p>Hello, World!</p></body></html>", sb.toString());
    }
}
