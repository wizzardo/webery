package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.TextField;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class TextFieldTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(TextField.class));
    }

    @Test
    public void test_1() {
        Node n = Node.parse("<g:textField name=\"myField\" value=\"${myValue}\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        model.put("myValue", 1);
        RenderResult result = l.get(model);

        Assert.assertEquals("<input type=\"text\" name=\"myField\" id=\"myField\" value=\"1\"/>\n", result.toString());
    }

    @Test
    public void test_2() {
        Node n = Node.parse("<g:textField name=\"myField\" id=\"text_${myValue}\" value=\"${myValue}\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        model.put("myValue", 1);
        RenderResult result = l.get(model);

        Assert.assertEquals("<input type=\"text\" name=\"myField\" id=\"text_1\" value=\"1\"/>\n", result.toString());
    }

    @Test
    public void test_3() {
        Node n = Node.parse("<g:textField name=\"myField_${myValue++}\" value=\"${myValue}\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        model.put("myValue", 1);
        RenderResult result = l.get(model);

        Assert.assertEquals("<input type=\"text\" name=\"myField_1\" id=\"myField_1\" value=\"2\"/>\n", result.toString());
    }

    @Test
    public void test_4() {
        Node n = Node.parse("<g:textField name=\"myField\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("<input type=\"text\" name=\"myField\" id=\"myField\"/>\n", result.toString());
    }

    @Test
    public void test_5() {
        Node n = Node.parse("<g:textField name=\"myField\" style=\"border: 0\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("<input type=\"text\" name=\"myField\" id=\"myField\" style=\"border: 0\"/>\n", result.toString());
    }
}
