package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.CheckBox;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 26.05.15.
 */
public class CheckBoxTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(CheckBox.class));
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<g:checkBox name=\"myCheckbox\" value=\"${true}\"/>").get(new Model());

        Assert.assertEquals("<input type=\"checkbox\" name=\"myCheckbox\" id=\"myCheckbox\" value=\"true\"/>\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderResult result = prepare("<g:checkBox name=\"myCheckbox\" id=\"myCheckbox_${i}\" checked=\"${true}\"/>")
                .get(new Model().append("i", 2));

        Assert.assertEquals("<input type=\"checkbox\" name=\"myCheckbox\" id=\"myCheckbox_2\" checked=\"checked\"/>\n", result.toString());
    }

    @Test
    public void test_3() {
        RenderResult result = prepare("<g:checkBox name=\"myCheckbox\" id=\"myCheckbox_${i}\" checked=\"true\"/>")
                .get(new Model().append("i", 3));

        Assert.assertEquals("<input type=\"checkbox\" name=\"myCheckbox\" id=\"myCheckbox_3\" checked=\"checked\"/>\n", result.toString());
    }

}
