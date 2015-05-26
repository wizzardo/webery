package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Join;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class JoinTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(Join.class));
    }

    public static class Book {
        public String title;

        public Book(String title) {
            this.title = title;
        }
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<g:join in=\"['Grails', 'Groovy', 'Gradle']\" delimiter=\"_\"/>").get(new Model());

        Assert.assertEquals("Grails_Groovy_Gradle\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderResult result = prepare("<g:join in=\"['Grails', 'Groovy', 'Gradle']\"/>").get(new Model());

        Assert.assertEquals("Grails, Groovy, Gradle\n", result.toString());
    }
}
