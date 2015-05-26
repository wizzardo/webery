package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Collect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class CollectTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(Collect.class));
    }

    public static class Book {
        public String title;

        public Book(String title) {
            this.title = title;
        }
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<div><g:collect in=\"${books}\" expr=\"${it.title}\">$it<br/></g:collect></div>")
                .get(new Model().append("books", Arrays.asList(new Book("Book one"), new Book("Book two"))));

        Assert.assertEquals("" +
                "<div>\n" +
                "        Book one\n" +
                "        <br/>\n" +
                "        Book two\n" +
                "        <br/>\n" +
                "</div>\n", result.toString());
    }
}
