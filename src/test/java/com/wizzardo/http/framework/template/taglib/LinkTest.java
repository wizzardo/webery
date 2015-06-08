package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Link;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by wizzardo on 26.04.15.
 */
public class LinkTest extends WebApplicationTest implements TagTest {

    public static class BookController extends Controller {

        public Renderer list() {
            return renderString("list of books");
        }

        public Renderer get() {
            return renderString("some book");
        }
    }

    @Override
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        super.setUp();
        TagLib.findTags(Collections.singletonList(Link.class));
        server.getUrlMapping()
                .append("/book/list", BookController.class, "list")
                .append("/book/$id", BookController.class, "get")
        ;
    }

    @Test
    public void testLinkTag() {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        Map<String, Object> model = new LinkedHashMap<String, Object>();

        attrs.put("controller", "book");
        attrs.put("action", "list");
        Assert.assertEquals("<a href=\"/book/list\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("absolute", "true");
        Assert.assertEquals("<a href=\"http://localhost:9999/book/list\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("base", "http://ya.ru");
        Assert.assertEquals("<a href=\"http://ya.ru/book/list\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("fragment", "some_fragment");
        Assert.assertEquals("<a href=\"http://ya.ru/book/list#some_fragment\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("params", "[key:'value']");
        Assert.assertEquals("<a href=\"http://ya.ru/book/list?key=value#some_fragment\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("params", "[key:'value', key2: 'value2']");
        Assert.assertEquals("<a href=\"http://ya.ru/book/list?key=value&key2=value2#some_fragment\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());


        attrs.clear();
        attrs.put("controller", "book");
        attrs.put("action", "get");
        attrs.put("params", "[id:1]");
        Assert.assertEquals("<a href=\"/book/1\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("class", "red");
        attrs.put("id", "link1");
        Assert.assertEquals("<a href=\"/book/1\" class=\"red\" id=\"link1\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());


        attrs.clear();
        attrs.put("controller", "book");
        attrs.put("action", "get");
        attrs.put("params", "[id:id]");
        model.put("id", 1);
        Assert.assertEquals("<a href=\"/book/1\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("id", "link_${id}");
        Assert.assertEquals("<a href=\"/book/1\" id=\"link_1\"/>\n", new Link().init(new LinkedHashMap<>(attrs)).get(model).toString());
    }

    @Test
    public void testInTemplate() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <a href=\"/book/1\">\n" +
                "        text\n" +
                "    </a>\n" +
                "</div>\n", prepare("<div><g:link controller=\"book\" action=\"get\" params=\"[id: 1]\">text</g:link></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_exception() {
        checkException(() -> prepare("<div><g:link controller=\"none\" action=\"index\" params=\"[id: 1]\">text</g:link></div>"),
                IllegalStateException.class, "can not find mapping for controller 'none' and action:'index'");
    }
}
