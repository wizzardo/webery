package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.TextArea;
import com.wizzardo.http.framework.template.taglib.g.TextField;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 18.05.15.
 */
public class TextAreaTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(TextArea.class));
    }

    @Test
    public void test_1() {
        Node n = Node.parse("<g:textArea name=\"myField\" value=\"${myValue}\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        model.put("myValue", 1);
        RenderResult result = l.get(model);

        Assert.assertEquals("<textarea name=\"myField\" id=\"myField\">1</textarea>", result.toString());
    }

    @Test
    public void test_2() {
        Node n = Node.parse("<g:textArea name=\"myField\">$myValue</g:textArea>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        model.put("myValue", 1);
        RenderResult result = l.get(model);

        Assert.assertEquals("<textarea name=\"myField\" id=\"myField\">" +
                "1" +
                "</textarea>", result.toString());
    }

}
