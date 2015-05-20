package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Else;
import com.wizzardo.http.framework.template.taglib.g.Elseif;
import com.wizzardo.http.framework.template.taglib.g.If;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 09.05.15.
 */
public class IfTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(If.class));
        TagLib.findTags(Collections.singletonList(Else.class));
        TagLib.findTags(Collections.singletonList(Elseif.class));
    }

    @Test
    public void test_1() {
        Node n = Node.parse("<div><g:if test=\"${flag}\">text</g:if></div>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        RenderResult result = l.get(model);

        Assert.assertEquals("" +
                "<div>\n" +
                "</div>\n", result.toString());

        model.clear();
        model.put("flag", true);
        result = l.get(model);

        Assert.assertEquals("" +
                "<div>\n" +
                "        text\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_2() {
        Node n = Node.parse("<div><g:if test=\"${flag}\">text</g:if><g:else>else</g:else></div>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        RenderResult result = l.get(model);

        Assert.assertEquals("" +
                "<div>\n" +
                "        else\n" +
                "</div>\n", result.toString());

        model.clear();
        model.put("flag", true);
        result = l.get(model);

        Assert.assertEquals("" +
                "<div>\n" +
                "        text\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_3() {
        Node n = Node.parse("<div><g:if test=\"${i == 1}\">one</g:if><g:elseif test=\"${i == 2}\">two</g:elseif><g:else>else</g:else></div>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        RenderResult result = l.get(model);

        Assert.assertEquals("" +
                "<div>\n" +
                "        else\n" +
                "</div>\n", result.toString());

        model.clear();
        model.put("i", 1);
        result = l.get(model);

        Assert.assertEquals("" +
                "<div>\n" +
                "        one\n" +
                "</div>\n", result.toString());

        model.clear();
        model.put("i", 2);
        result = l.get(model);

        Assert.assertEquals("" +
                "<div>\n" +
                "        two\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_4() {
        Node n = Node.parse("\"<div><g:if test=\\\"${flag}\\\">text</g:if>error<g:else>foo</g:else></div>\"", true);
        try {
            ViewRenderer.prepare(n.children(), new RenderableList(), "", "");
            assert false;
        } catch (IllegalStateException e) {
            Assert.assertEquals("If tag must be before Else tag", e.getMessage());
        }

        n = Node.parse("\"<div><g:if test=\\\"${flag}\\\">text</g:if>error<g:elseif>foo</g:elseif></div>\"", true);
        try {
            ViewRenderer.prepare(n.children(), new RenderableList(), "", "");
            assert false;
        } catch (IllegalStateException e) {
            Assert.assertEquals("If tag must be before Else tag", e.getMessage());
        }
    }
}
