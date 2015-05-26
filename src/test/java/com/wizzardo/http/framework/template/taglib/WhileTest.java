package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.While;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class WhileTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(While.class));
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<div><g:while test=\"${++i <= 3}\"><p>Current i = ${i}</p></g:while></div>")
                .get(new Model().append("i", 0));

        Assert.assertEquals("" +
                "<div>\n" +
                "        <p>\n" +
                "            Current i = 1\n" +
                "        </p>\n" +
                "        <p>\n" +
                "            Current i = 2\n" +
                "        </p>\n" +
                "        <p>\n" +
                "            Current i = 3\n" +
                "        </p>\n" +
                "</div>\n", result.toString());
    }
}
