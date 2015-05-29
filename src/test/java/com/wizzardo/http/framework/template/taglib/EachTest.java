package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Each;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class EachTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(Each.class));
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<div><g:each in=\"[1,2,3]\" var=\"i\">$i<br/></g:each></div>").get(new Model());

        Assert.assertEquals("" +
                "<div>\n" +
                "    1\n" +
                "    <br/>\n" +
                "    2\n" +
                "    <br/>\n" +
                "    3\n" +
                "    <br/>\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderResult result = prepare("<div><g:each in=\"$list\">$it<br/></g:each></div>")
                .get(new Model().append("list", Arrays.asList(1, 2, 3)));

        Assert.assertEquals("" +
                "<div>\n" +
                "    1\n" +
                "    <br/>\n" +
                "    2\n" +
                "    <br/>\n" +
                "    3\n" +
                "    <br/>\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_3() {
        RenderResult result = prepare("<div><g:each in=\"$list\" status=\"i\">${i+1}: $it<br/></g:each></div>")
                .get(new Model().append("list", Arrays.asList("one", "two", "three")));

        Assert.assertEquals("" +
                "<div>\n" +
                "    1: one\n" +
                "    <br/>\n" +
                "    2: two\n" +
                "    <br/>\n" +
                "    3: three\n" +
                "    <br/>\n" +
                "</div>\n", result.toString());
    }
}
