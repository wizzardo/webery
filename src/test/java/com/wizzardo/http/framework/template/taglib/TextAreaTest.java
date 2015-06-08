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
public class TextAreaTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(TextArea.class));
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<g:textArea name=\"myField\" value=\"${myValue}\"/>")
                .get(new Model().append("myValue", 1));

        Assert.assertEquals("<textarea name=\"myField\" id=\"myField\">1</textarea>\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderResult result = prepare("<g:textArea name=\"myField\">$myValue</g:textArea>")
                .get(new Model().append("myValue", 2));

        Assert.assertEquals("<textarea name=\"myField\" id=\"myField\">" +
                "2" +
                "</textarea>\n", result.toString());
    }

    @Test
    public void test_3() {
        RenderResult result = prepare("<g:textArea name=\"myField\" id=\"myId\">$myValue</g:textArea>")
                .get(new Model().append("myValue", 3));

        Assert.assertEquals("<textarea name=\"myField\" id=\"myId\">" +
                "3" +
                "</textarea>\n", result.toString());
    }

}
