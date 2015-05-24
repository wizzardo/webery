package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.FormatBoolean;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 24.05.15.
 */
public class FormatBooleanTest extends WebApplicationTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(FormatBoolean.class));
    }

    @Test
    public void test_1() {
        Node n = Node.parse("<div><g:formatBoolean boolean=\"${myBoolean}\"/></div>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        model.put("myBoolean", true);
        RenderResult result = l.get(model);

        Assert.assertEquals("" +
                "<div>\n" +
                "    true\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_2() {
        Node n = Node.parse("<g:formatBoolean boolean=\"${i > 0}\" true=\"$i > 0\" false=\"$i <= 0\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();

        model.put("i", 1);
        Assert.assertEquals("1 > 0\n", l.get(model).toString());

        model.put("i", -1);
        Assert.assertEquals("-1 <= 0\n", l.get(model).toString());
    }
}
