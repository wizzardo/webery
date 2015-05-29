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
public class TextFieldTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(TextField.class));
    }

    protected String getType() {
        return "text";
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<g:" + getType() + "Field name=\"myField\" value=\"${myValue}\"/>")
                .get(new Model().append("myValue", 1));

        Assert.assertEquals("<input type=\"" + getType() + "\" name=\"myField\" id=\"myField\" value=\"1\"/>\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderResult result = prepare("<g:" + getType() + "Field name=\"myField\" id=\"text_${myValue}\" value=\"${myValue}\"/>")
                .get(new Model().append("myValue", 1));

        Assert.assertEquals("<input type=\"" + getType() + "\" name=\"myField\" id=\"text_1\" value=\"1\"/>\n", result.toString());
    }

    @Test
    public void test_3() {
        RenderResult result = prepare("<g:" + getType() + "Field name=\"myField_${myValue++}\" value=\"${myValue}\"/>")
                .get(new Model().append("myValue", 1));

        Assert.assertEquals("<input type=\"" + getType() + "\" name=\"myField_1\" id=\"myField_1\" value=\"2\"/>\n", result.toString());
    }

    @Test
    public void test_4() {
        RenderResult result = prepare("<g:" + getType() + "Field name=\"myField\"/>").get(new Model());

        Assert.assertEquals("<input type=\"" + getType() + "\" name=\"myField\" id=\"myField\"/>\n", result.toString());
    }

    @Test
    public void test_5() {
        RenderResult result = prepare("<g:" + getType() + "Field name=\"myField\" style=\"border: 0\"/>").get(new Model());

        Assert.assertEquals("<input type=\"" + getType() + "\" name=\"myField\" id=\"myField\" style=\"border: 0\"/>\n", result.toString());
    }
}
