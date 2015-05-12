package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Collect;
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
public class JoinTest {

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
        Node n = Node.parse("<g:join in=\"['Grails', 'Groovy', 'Gradle']\" delimiter=\"_\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("Grails_Groovy_Gradle", result.toString());
    }

    @Test
    public void test_2() {
        Node n = Node.parse("<g:join in=\"['Grails', 'Groovy', 'Gradle']\"/>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("Grails, Groovy, Gradle", result.toString());
    }
}
