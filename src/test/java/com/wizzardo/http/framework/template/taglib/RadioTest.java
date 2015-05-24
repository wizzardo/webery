package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Collect;
import com.wizzardo.http.framework.template.taglib.g.Radio;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class RadioTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(Radio.class));
    }

    @Test
    public void test_1() {
        Node n = Node.parse("<g:radio name=\"myGroup\" value=\"1\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup\" value=\"1\"/>\n", result.toString());
    }

    @Test
    public void test_2() {
        Node n = Node.parse("<g:radio name=\"myGroup\" checked=\"false\" value=\"2\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup\" value=\"2\"/>\n", result.toString());
    }

    @Test
    public void test_3() {
        Node n = Node.parse("<g:radio name=\"myGroup\" checked=\"true\" value=\"3\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup\" value=\"3\" checked=\"checked\"/>\n", result.toString());
    }

    @Test
    public void test_4() {
        Node n = Node.parse("<g:radio name=\"myGroup_${1}\" checked=\"${3>2}\" value=\"${2+2}\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup_1\" value=\"4\" checked=\"checked\"/>\n", result.toString());
    }

    @Test
    public void test_5() {
        Node n = Node.parse("<g:radio name=\"myGroup\" value=\"5\" style=\"border: 0\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup\" value=\"5\" style=\"border: 0\"/>\n", result.toString());
    }
}
