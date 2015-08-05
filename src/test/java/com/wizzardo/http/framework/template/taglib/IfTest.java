package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Else;
import com.wizzardo.http.framework.template.taglib.g.Elseif;
import com.wizzardo.http.framework.template.taglib.g.If;
import com.wizzardo.tools.xml.GspParser;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 09.05.15.
 */
public class IfTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(If.class));
        TagLib.findTags(Collections.singletonList(Else.class));
        TagLib.findTags(Collections.singletonList(Elseif.class));
    }

    @Test
    public void test_1() {
        RenderableList l = prepare("<div><g:if test=\"${flag}\">text</g:if></div>");
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
        RenderableList l = prepare("<div><g:if test=\"${flag}\">text</g:if><g:else>else</g:else></div>");
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
        RenderableList l = prepare("<div><g:if test=\"${i == 1}\">one</g:if><g:elseif test=\"${i == 2}\">two</g:elseif><g:else>else</g:else></div>");
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
        RenderableList l = prepare("<g:if test=\"${i == 1}\">one</g:if>" +
                "<g:elseif test=\"${i == 2}\">two</g:elseif>" +
                "<g:elseif test=\"${i == 3}\">three</g:elseif>" +
                "<g:else>else</g:else>");
        Model model = new Model();

        RenderResult result = l.get(model);
        Assert.assertEquals("    else\n", result.toString());

        model.clear();
        model.put("i", 1);
        result = l.get(model);
        Assert.assertEquals("    one\n", result.toString());

        model.clear();
        model.put("i", 2);
        result = l.get(model);
        Assert.assertEquals("    two\n", result.toString());

        model.clear();
        model.put("i", 3);
        result = l.get(model);
        Assert.assertEquals("    three\n", result.toString());
    }

    @Test
    public void test_exceptions() {
        Node n = new GspParser().parse("<div><g:if test=\"${flag}\">text</g:if>error<g:else>foo</g:else></div>");
        try {
            ViewRenderer.prepare(n.children(), new RenderableList(), "", "");
            assert false;
        } catch (IllegalStateException e) {
            Assert.assertEquals("If tag must be before Else tag", e.getMessage());
        }

        n = new GspParser().parse("<div><g:if test=\"${flag}\">text</g:if>error<g:elseif test=\"${flag}\">foo</g:elseif></div>");
        try {
            ViewRenderer.prepare(n.children(), new RenderableList(), "", "");
            assert false;
        } catch (IllegalStateException e) {
            Assert.assertEquals("If tag must be before Else tag", e.getMessage());
        }
    }
}
