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
public class RadioTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(Radio.class));
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<g:radio name=\"myGroup\" value=\"1\"/>").get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup\" id=\"myGroup\" value=\"1\"/>\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderResult result = prepare("<g:radio name=\"myGroup\" checked=\"false\" value=\"2\"/>").get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup\" id=\"myGroup\" value=\"2\"/>\n", result.toString());
    }

    @Test
    public void test_3() {
        RenderResult result = prepare("<g:radio name=\"myGroup\" checked=\"true\" value=\"3\"/>").get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup\" id=\"myGroup\" value=\"3\" checked=\"checked\"/>\n", result.toString());
    }

    @Test
    public void test_4() {
        RenderResult result = prepare("<g:radio name=\"myGroup_${1}\" checked=\"${3>2}\" value=\"${2+2}\"/>").get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup_1\" id=\"myGroup_1\" value=\"4\" checked=\"checked\"/>\n", result.toString());
    }

    @Test
    public void test_5() {
        RenderResult result = prepare("<g:radio name=\"myGroup\" id=\"radio_id\" value=\"5\" style=\"border: 0\"/>").get(new Model());

        Assert.assertEquals("<input type=\"radio\" name=\"myGroup\" id=\"radio_id\" value=\"5\" style=\"border: 0\"/>\n", result.toString());
    }
}
