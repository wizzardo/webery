package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
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
}
