package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.CheckBox;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 26.05.15.
 */
public class CheckBoxTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(CheckBox.class));
    }

    @Test
    public void test_1() {
        Node n = Node.parse("<g:checkBox name=\"myCheckbox\" value=\"${true}\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("<input type=\"checkbox\" name=\"myCheckbox\" id=\"myCheckbox\" value=\"true\"/>\n", result.toString());
    }

    @Test
    public void test_2() {
        Node n = Node.parse("<g:checkBox name=\"myCheckbox\" id=\"myCheckbox_${i}\" checked=\"${true}\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        Model model = new Model();
        model.put("i", 1);
        RenderResult result = l.get(model);

        Assert.assertEquals("<input type=\"checkbox\" name=\"myCheckbox\" id=\"myCheckbox_1\" checked=\"checked\"/>\n", result.toString());
    }

}
