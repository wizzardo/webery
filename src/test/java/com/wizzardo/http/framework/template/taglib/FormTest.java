package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.template.Model;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.framework.template.TagLib;
import com.wizzardo.http.framework.template.taglib.g.Link;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 02.07.15.
 */
public class FormTest extends WebApplicationTest implements TagTest {

    public static class BookController extends Controller {

        public Renderer save() {
            return renderString("save");
        }

    }

    @Override
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        super.setUp();
        TagLib.findTags(Collections.singletonList(Link.class));
        server.getUrlMapping()
                .append("/book/save", BookController.class, "save")
        ;
    }


    @Test
    public void test_1() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <form action=\"/book/save\" method=\"POST\">\n" +
                "        text\n" +
                "    </form>\n" +
                "</div>\n", prepare("<div><g:form controller=\"book\" action=\"save\">text</g:form></div>")
                .get(new Model()).toString());
    }
}
